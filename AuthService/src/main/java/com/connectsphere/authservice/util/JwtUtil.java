package com.connectsphere.authservice.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.access-token-expiry-ms:900000}")
	private long accessTokenExpiry;

	@Value("${jwt.refresh-token-expiry-ms:604800000}")
	private long refreshTokenExpiry;

	private final Set<String> tokenDenylist = ConcurrentHashMap.newKeySet();

	public String generateAccessToken(String email, String role, int userId) {
		return Jwts.builder().setSubject(email).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiry)).claim("type", "access")
				.claim("role", role).claim("userId", userId).signWith(getSigningKey(), SignatureAlgorithm.HS512)
				.compact();
	}

	public String generateRefreshToken(String email) {
		return Jwts.builder().setSubject(email).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiry)).claim("type", "refresh")
				.signWith(getSigningKey(), SignatureAlgorithm.HS512).compact();
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			if (tokenDenylist.contains(token)) {
				log.warn("Denylisted token attempted");
				return false;
			}
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.warn("Unsupported JWT: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.warn("Malformed JWT: {}", e.getMessage());
		} catch (SecurityException e) {
			log.warn("Bad JWT signature: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.warn("Empty JWT: {}", e.getMessage());
		}
		return false;
	}

	public String extractEmail(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractRole(String token) {
		return parseClaims(token).get("role", String.class);
	}

	public int extractUserId(String token) {
		Object id = parseClaims(token).get("userId");
		if (id instanceof Integer)
			return (Integer) id;
		if (id instanceof Long)
			return ((Long) id).intValue();
		return 0;
	}

	public Date extractExpiry(String token) {
		return parseClaims(token).getExpiration();
	}

	public void denylistToken(String token) {
		tokenDenylist.add(token);
	}

	public boolean isDenylisted(String token) {
		return tokenDenylist.contains(token);
	}

	private Claims parseClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
}