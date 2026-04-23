package com.connectsphere.authservice.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.connectsphere.authservice.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtUtil jwtUtil;

	@Value("${jwt.cookie.access-token-name:cs_access_token}")
	private String accessCookieName;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = extractToken(request);
		if (token != null && jwtUtil.validateToken(token)) {
			int userId = jwtUtil.extractUserId(token);
			String email = jwtUtil.extractEmail(token);
			String role = jwtUtil.extractRole(token);

			SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role != null ? role : "ROLE_USER");
			Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
			SecurityContextHolder.getContext().setAuthentication(auth);
			log.debug("Authenticated via {}: {}", hasCookieToken(request) ? "cookie" : "bearer header", email);
		}
		filterChain.doFilter(request, response);
	}

	private String extractToken(HttpServletRequest request) {
		String fromCookie = extractCookie(request, accessCookieName);
		if (StringUtils.hasText(fromCookie))
			return fromCookie;

		String header = request.getHeader("Authorization");
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7);
		}

		return null;
	}

	private boolean hasCookieToken(HttpServletRequest request) {
		return StringUtils.hasText(extractCookie(request, accessCookieName));
	}

	private String extractCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null)
			return null;
		return Arrays.stream(request.getCookies()).filter(c -> name.equals(c.getName())).map(Cookie::getValue)
				.findFirst().orElse(null);
	}

}
