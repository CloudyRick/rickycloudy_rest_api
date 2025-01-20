package dev.rickcloudy.restapi.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${ACCESS_TOKEN_SECRET}")
    private String accessTokenSecret;
    @Value("${REFRESH_TOKEN_SECRET}")
    private String refreshTokenSecret;
    @Value("${ACCESS_TOKEN_EXPIRATION_MS}")
    private long accessTokenExpirationMs;
    @Value("${REFRESH_TOKEN_EXPIRATION_MS}")
    private long refreshTokenExpirationMs;
    private Logger log = LogManager.getLogger(JwtUtils.class);

    private Key getKeyFromSecret(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String username) {
        Key secretKey = getKeyFromSecret(accessTokenSecret);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Key secretKey = getKeyFromSecret(refreshTokenSecret);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, getKeyFromSecret(accessTokenSecret));
    }

    public boolean validateRefreshToken(String token) {
        log.debug("JwtUtils::validateRefreshToken::reached ");
        return validateToken(token, getKeyFromSecret(refreshTokenSecret));
    }

    private boolean validateToken(String token, Key secretKey) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // Invalid token
        }
    }

    public String extractUsernameFromAccessToken(String token) {
        return extractUsername(token, getKeyFromSecret(accessTokenSecret));
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractUsername(token, getKeyFromSecret(refreshTokenSecret));
    }

    private String extractUsername(String token, Key secretKey) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
