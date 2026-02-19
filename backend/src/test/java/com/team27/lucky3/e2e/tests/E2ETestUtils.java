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
 * Bunch of helpers for E2E tests. We use this to load .env settings and
 * sweep the database clean so our tests don't step on each other's toes.
 */
public final class E2ETestUtils {

    private E2ETestUtils() {
        // Just a utility class, no need to instantiate.
    }

    /**
     * Tries to grab configuration from the .env file.
     * If it's not where we expect, we hunt around in the parent directories.
     * Falls back to system variables if the file is missing or doesn't have what we need.
     *
     * @param requiredKeys keys we absolutely need to have
     * @return a map with all the environment settings
     */
    public static Map<String, String> loadEnvFile(String... requiredKeys) throws IOException {
        Map<String, String> env = new HashMap<>();
        Path envFile = Paths.get("").toAbsolutePath().resolve(".env");
        if (!Files.exists(envFile)) {
            // If we're running from the root, it might be in the backend folder.
            envFile = Paths.get("").toAbsolutePath().getParent().resolve("backend").resolve(".env");
        }
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                // Skip comments and empty lines.
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    env.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
        }
        for (String key : requiredKeys) {
            // Check system environment if it's missing or empty in the .env file.
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
     * Deletes reviews for a specific ride directly via JDBC.
     * This is way faster than using the UI and ensures we have a blank slate 
     * for the next test run. We skip silently if the DB info is missing.
     *
     * @param env   the environment settings (we need DB_URL, etc.)
     * @param rideId the ride we want to clean up
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
