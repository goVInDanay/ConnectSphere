package com.connectsphere.gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import com.connectsphere.gateway.util.JwtUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

	private final JwtUtil jwtUtil;
	private final AntPathMatcher matcher = new AntPathMatcher();

	public JwtAuthFilter(JwtUtil jwtUtil) {
		super(Config.class);
		this.jwtUtil = jwtUtil;
	}

	@Getter
	@Setter
	public static class Config {
		private List<String> openPaths;
	}

	@Override
	public GatewayFilter apply(Config config) {

		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();
			String method = request.getMethod().name();

			log.debug("Gateway: {} {}", method, path);

			if (isOpen(method, path, config.getOpenPaths())) {
				return chain.filter(exchange);
			}

			String token = extractToken(request);
			if (token == null) {
				return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing Bearer token");
			}

			if (!jwtUtil.isValid(token)) {
				return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
			}

			String userId = jwtUtil.extractUserId(token);
			String role = jwtUtil.extractRole(token);
			String email = jwtUtil.extractEmail(token);

			ServerHttpRequest mutatedRequest = request.mutate().header("X-User-Id", userId).header("X-User-Role", role)
					.header("X-User-Email", email).build();

			return chain.filter(exchange.mutate().request(mutatedRequest).build());
		};
	}

	private boolean isOpen(String method, String path, List<String> openPaths) {
		if (openPaths == null || openPaths.isEmpty())
			return false;

		return openPaths.stream().anyMatch(pattern -> {
			if (pattern.contains(":")) {
				String[] parts = pattern.split(":", 2);
				return parts[0].equalsIgnoreCase(method) && matcher.match(parts[1].trim(), path);
			}
			return matcher.match(pattern, path);
		});
	}

	private String extractToken(ServerHttpRequest request) {
		String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
	}

	private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
		ServerHttpResponse response = exchange.getResponse();

		response.setStatusCode(status);
		response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		response.getHeaders().set("X-Auth-Error", message);

		String body = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status.value(),
				status.getReasonPhrase(), message);

		return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
	}
}