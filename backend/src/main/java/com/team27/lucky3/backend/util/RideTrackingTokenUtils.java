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
 * Utility class for generating and validating JWT tokens for ride tracking.
 * These tokens allow linked passengers (registered or not) to track rides.
 * Tokens do not have a time expiration - they are invalidated when the ride ends.
 */
@Component
public class RideTrackingTokenUtils {

    @Value("${jwt.secret}")
    private String SECRET;

    private static final String ISSUER = "lucky3-tracking";
    private static final String TOKEN_TYPE = "ride-tracking";
    // Set a very long expiration (1 year) - actual validity is controlled by ride status
    private static final long ONE_YEAR_MS = 365L * 24 * 60 * 60 * 1000;
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    /**
     * Generates a JWT token for ride tracking.
     * Token contains rideId and email of the linked passenger.
     */
    public String generateTrackingToken(Long rideId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ONE_YEAR_MS);

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject(TOKEN_TYPE)
                .claim("rideId", rideId)
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getKey(), SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Validates the tracking token and returns the claims if valid.
     * Returns null if token is invalid or expired.
     */
    public Claims validateAndGetClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Verify this is a tracking token
            if (!ISSUER.equals(claims.getIssuer()) || !TOKEN_TYPE.equals(claims.getSubject())) {
                return null;
            }
            
            return claims;
        } catch (ExpiredJwtException ex) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts ride ID from the tracking token.
     */
    public Long getRideIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("rideId", Long.class);
    }

    /**
     * Extracts email from the tracking token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        if (claims == null) return null;
        return claims.get("email", String.class);
    }

    /**
     * Checks if the token structure is valid (properly signed).
     * Note: This does NOT check if the ride is still trackable.
     */
    public boolean isTokenStructureValid(String token) {
        return validateAndGetClaims(token) != null;
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
