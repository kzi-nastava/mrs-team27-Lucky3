package com.team27.lucky3.e2e.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared utilities for E2E tests — .env loading and database cleanup.
 * Extracted to avoid duplicating this logic across test classes.
 */
public final class E2ETestUtils {

    private E2ETestUtils() {
        // Utility class
    }

    /**
     * Reads the backend .env file and falls back to system environment variables.
     * Searches for .env in the current directory first, then in the parent/backend directory.
     *
     * @param requiredKeys keys to look up from system env if missing from .env
     * @return map of key-value environment pairs
     */
    public static Map<String, String> loadEnvFile(String... requiredKeys) throws IOException {
        Map<String, String> env = new HashMap<>();
        Path envFile = Paths.get("").toAbsolutePath().resolve(".env");
        if (!Files.exists(envFile)) {
            envFile = Paths.get("").toAbsolutePath().getParent().resolve("backend").resolve(".env");
        }
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    env.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
        }
        for (String key : requiredKeys) {
            if (!env.containsKey(key) || env.get(key).isBlank()) {
                String sysVal = System.getenv(key);
                if (sysVal != null && !sysVal.isBlank()) {
                    env.put(key, sysVal);
                }
            }
        }
        return env;
    }

    /**
     * Removes all reviews for a specific ride directly from the database via JDBC.
     * Silently skips if database credentials are missing.
     *
     * @param env   environment map containing DB_URL, DB_USERNAME, DB_PASSWORD
     * @param rideId the ride ID whose reviews should be deleted
     */
    public static void cleanupReviewsForRide(Map<String, String> env, long rideId) throws Exception {
        String dbUrl = env.get("DB_URL");
        String dbUser = env.get("DB_USERNAME");
        String dbPass = env.get("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            System.err.println("Database info is missing, skipping review cleanup for ride " + rideId);
            return;
        }
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM review WHERE ride_id = ?")) {
            ps.setLong(1, rideId);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("Deleted " + deleted + " old review(s) for ride " + rideId);
            }
        }
    }
}
