package com.connectsphere.commentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.connectsphere.commentservice.util.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/comments/post/**", "/api/comments/*/replies", "/api/comments/user/**",
						"/api/comments/*", "/api/comments/post/*/count")
				.permitAll().requestMatchers("/api/comments", // POST
						"/api/comments/*/replies", "/api/comments/*", "/api/comments/*/like", "/api/comments/*/unlike")
				.authenticated()
				.requestMatchers("/swagger-ui/**", "/api/swagger-ui/**", "/api/swagger-ui.html", "/v3/api-docs/**",
						"/v3/api-docs", "/webjars/**", "/swagger-resources/**", "/configuration/**")
				.permitAll().anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
	}
}