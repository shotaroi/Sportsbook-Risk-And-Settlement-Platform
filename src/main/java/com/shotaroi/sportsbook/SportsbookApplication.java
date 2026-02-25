package com.shotaroi.sportsbook;

import com.shotaroi.sportsbook.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SportsbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsbookApplication.class, args);
    }
}
