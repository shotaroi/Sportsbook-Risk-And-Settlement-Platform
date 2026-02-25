package com.shotaroi.sportsbook.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simplified auth for portfolio: exchange customerId for JWT.
 * In production, use proper OAuth2/OIDC or credential-based login.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Get JWT token for API access")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    @Operation(summary = "Get JWT token (MVP: pass customerId as subject)")
    public ResponseEntity<Map<String, String>> getToken(@RequestBody Map<String, Long> body) {
        Long customerId = body.get("customerId");
        if (customerId == null) {
            return ResponseEntity.badRequest().build();
        }
        String token = jwtService.generateToken(String.valueOf(customerId));
        return ResponseEntity.ok(Map.of("token", token));
    }
}
