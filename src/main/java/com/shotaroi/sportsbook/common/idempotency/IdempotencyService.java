package com.shotaroi.sportsbook.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shotaroi.sportsbook.common.domain.IdempotencyScope;
import com.shotaroi.sportsbook.common.entity.IdempotencyKey;
import com.shotaroi.sportsbook.common.error.DuplicateIdempotencyKeyException;
import com.shotaroi.sportsbook.common.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Idempotency: same (scope, scopeId, idempotencyKey) returns cached response.
 * Different request hash with same key = DuplicateIdempotencyKeyException.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute idempotently. If key exists with same request hash, return cached response.
     * If key exists with different hash, throw DuplicateIdempotencyKeyException.
     */
    @Transactional
    public <T> T execute(
            IdempotencyScope scope,
            String scopeId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            IdempotentOperation<T> operation
    ) {
        String requestHash = hashRequest(request);

        var existing = repository.findByScopeAndScopeIdAndIdempotencyKey(scope, scopeId, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (!key.getRequestHash().equals(requestHash)) {
                throw new DuplicateIdempotencyKeyException(idempotencyKey, scope.name());
            }
            // Same request: return cached response (no re-execution)
            if (key.getResponseJson() != null) {
                try {
                    T cached = objectMapper.readValue(key.getResponseJson(), responseType);
                    log.debug("Idempotent replay: scope={}, scopeId={}, key={}", scope, scopeId, idempotencyKey);
                    return cached;
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Failed to deserialize cached idempotency response", e);
                }
            }
        }

        T result = operation.execute();

        var toSave = new IdempotencyKey();
        toSave.setScope(scope);
        toSave.setScopeId(scopeId);
        toSave.setIdempotencyKey(idempotencyKey);
        toSave.setRequestHash(requestHash);
        toSave.setResponseJson(serializeResponse(result));
        toSave.setStatus("COMPLETED");

        try {
            repository.save(toSave);
        } catch (Exception e) {
            // Concurrent insert - another thread may have saved; verify we're consistent
            var retry = repository.findByScopeAndScopeIdAndIdempotencyKey(scope, scopeId, idempotencyKey);
            if (retry.isPresent() && retry.get().getRequestHash().equals(requestHash)) {
                return result;
            }
            throw e;
        }

        return result;
    }

    private String hashRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash request", e);
        }
    }

    private String serializeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize response for idempotency cache");
            return null;
        }
    }

    @FunctionalInterface
    public interface IdempotentOperation<T> {
        T execute();
    }
}
