package com.connectsphere.gateway.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		ServerHttpResponse response = exchange.getResponse();
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		response.getHeaders().set("X-Error-Gateway", "GatewayError");
		HttpStatus status;
		String message;

		if (ex instanceof NotFoundException || ex.getMessage().contains("Unable to find instance")) {
			status = HttpStatus.SERVICE_UNAVAILABLE;
			message = "The requested service is temporarily unavailable. Please try again shortly.";
			log.error("Gateway: service not found — {}", ex.getMessage());
		} else if (ex instanceof ResponseStatusException rse) {
			status = HttpStatus.valueOf(rse.getStatusCode().value());
			message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
		} else {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = "An unexpected error occurred in the gateway.";
			log.error("Gateway: unhandled exception", ex);
		}
		response.setStatusCode(status);
		String body = String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", status.value(),
				status.getReasonPhrase(), message);

		byte[] bytes = body.getBytes();
		return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
	}
}