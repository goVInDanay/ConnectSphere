package com.connectsphere.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("Gateway: JWT expired");
		} catch (JwtException e) {
			log.warn("Gateway: JWT invalid — {}", e.getMessage());
		}
		return false;
	}

	public String extractUserId(String token) {
		Object id = parseClaims(token).get("userId");
		if (id == null)
			return "0";
		return String.valueOf(id);
	}

	public String extractRole(String token) {
		String role = parseClaims(token).get("role", String.class);
		return role != null ? role : "ROLE_USER";
	}

	public String extractEmail(String token) {
		return parseClaims(token).getSubject();
	}

	private Claims parseClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
				.build().parseClaimsJws(token).getBody();
	}
}