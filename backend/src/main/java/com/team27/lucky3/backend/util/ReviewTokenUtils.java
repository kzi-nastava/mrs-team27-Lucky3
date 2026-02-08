package com.team27.lucky3.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utility class for generating and validating JWT tokens for ride review links.
 * Tokens are valid for 3 days after ride completion.
 */
@Component
public class ReviewTokenUtils {

    @Value("${jwt.secret}")
    private String SECRET;

    private static final String ISSUER = "lucky3-review";
    private static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000; // 3 days in milliseconds
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    /**
     * Generates a JWT token for ride review (registered passenger).
     * Token contains rideId, passengerId, and driverId and expires in 3 days.
     */
    public String generateReviewToken(Long rideId, Long passengerId, Long driverId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + THREE_DAYS_MS);

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject("review")
                .claim("rideId", rideId)
                .claim("passengerId", passengerId)
                .claim("driverId", driverId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Generates a JWT token for ride review (linked / non-registered passenger).
     * Token contains rideId, reviewerEmail, and driverId and expires in 3 days.
     */
    public String generateReviewTokenForEmail(Long rideId, String reviewerEmail, Long driverId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + THREE_DAYS_MS);

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject("review")
                .claim("rideId", rideId)
                .claim("reviewerEmail", reviewerEmail)
                .claim("driverId", driverId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Validates the review token and returns the claims if valid.
     * Returns null if token is invalid or expired.
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            // Token has expired
            return null;
        } catch (Exception e) {
            // Invalid token
            return null;
        }
    }

    /**
     * Extracts ride ID from the review token.
     */
    public Long getRideIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("rideId", Long.class);
    }

    /**
     * Extracts passenger ID from the review token.
     */
    public Long getPassengerIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("passengerId", Long.class);
    }

    /**
     * Extracts driver ID from the review token.
     */
    public Long getDriverIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("driverId", Long.class);
    }

    /**
     * Extracts reviewer email from the review token (for linked/non-registered passengers).
     * Returns null if not present (i.e. registered-passenger token).
     */
    public String getReviewerEmailFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("reviewerEmail", String.class);
    }

    /**
     * Checks if the token is valid (not expired and properly signed).
     */
    public boolean isTokenValid(String token) {
        return validateAndGetClaims(token) != null;
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
