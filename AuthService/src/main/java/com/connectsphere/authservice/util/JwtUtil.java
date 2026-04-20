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
import io.jsonwebtoken.JwtBuilder;
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

	@Value("${jwt.access-token-expiry}")
	private Long accessTokenExpiry;

	@Value("${jwt.refresh-token-expiry}")
	private Long refreshTokenExpiry;

	private final Set<String> tokenDenylist = ConcurrentHashMap.newKeySet();

	public String generateAccessToken(String email, String role) {
		return buildToken(email, role, accessTokenExpiry, "access");
	}

	public String generateRefreshToken(String email) {
		return buildToken(email, null, refreshTokenExpiry, "refresh");
	}

	private String buildToken(String subject, String role, Long expiry, String tokenType) {
		JwtBuilder builder = Jwts.builder().setSubject(subject).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiry)).claim("type", tokenType)
				.signWith(getSigningKey(), SignatureAlgorithm.HS512);
		if (role != null) {
			builder.claim("role", role);
		}
		return builder.compact();
	}

	public boolean validateToken(String token) {
		try {
			Claims claims = parseClaims(token);
			if (tokenDenylist.contains(token)) {
				log.warn("Denylisted token used: {}", token.substring(0, 20));
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
			log.warn("Invalid JWT signature: {}", e.getMessage());
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

	public Date extractExpiry(String token) {
		return parseClaims(token).getExpiration();
	}

	private Claims parseClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
	}

	public void denylistToken(String token) {
		tokenDenylist.add(token);
	}

	public boolean isDenylisted(String token) {
		return tokenDenylist.contains(token);
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
