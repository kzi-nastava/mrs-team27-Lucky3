package com.team27.lucky3.backend.util;

import com.team27.lucky3.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TokenUtils {

    @Value("lucky3-app")
    private String APP_NAME;

    @Value("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
    public String SECRET;

    @Value("1800000") // 30 min
    private int EXPIRES_IN;

    @Value("Authorization")
    private String AUTH_HEADER;

    private static final String AUDIENCE_WEB = "web";
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    // --- Generate Token ---
    public String generateToken(User user) {
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(user.getEmail()) // Explicitly using Email as Subject
                .setAudience(AUDIENCE_WEB)
                .setIssuedAt(new Date())
                .claim("role", user.getRole().name())
                .claim("id", user.getId())
                .setExpiration(new Date(new Date().getTime() + EXPIRES_IN))
                .signWith(SIGNATURE_ALGORITHM, SECRET).compact();
    }

    // --- Get Token from Request ---
    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // --- Get Email from Token ---
    // RENAMED: was getUsernameFromToken
    public String getEmailFromToken(String token) {
        String email;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            email = claims.getSubject();
        } catch (Exception e) {
            email = null;
        }
        return email;
    }

    // --- Get Expiration ---
    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    private Claims getAllClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    // --- Validate Token ---
    public Boolean validateToken(String token, UserDetails userDetails) {
        // Cast to our concrete User class to access getEmail() directly
        User user = (User) userDetails;
        final String tokenEmail = getEmailFromToken(token);
        final Date created = getIssuedAtDateFromToken(token);

        return (tokenEmail != null
                && tokenEmail.equals(user.getEmail()) // Explicitly compare Emails
                && !isCreatedBeforeLastPasswordReset(created, user.getLastPasswordResetDate()));
    }

    private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {
        return (lastPasswordReset != null && created.before(lastPasswordReset));
    }

    public Date getIssuedAtDateFromToken(String token) {
        Date issueAt;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            issueAt = claims.getIssuedAt();
        } catch (Exception e) {
            issueAt = null;
        }
        return issueAt;
    }

    public int getExpiredIn() {
        return EXPIRES_IN;
    }
}