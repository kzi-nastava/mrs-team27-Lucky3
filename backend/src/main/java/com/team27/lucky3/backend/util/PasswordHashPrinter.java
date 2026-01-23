package com.team27.lucky3.backend.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordHashPrinter {

    @Bean
    public CommandLineRunner printAdminPasswordHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String raw = "admin123";
            String hash = passwordEncoder.encode(raw);

            System.out.println("\n==============================");
            System.out.println("RAW:  " + raw);
            System.out.println("HASH: " + hash);
            System.out.println("==============================\n");
        };
    }
}
