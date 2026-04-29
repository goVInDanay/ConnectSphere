package com.connectsphere.media.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.connectsphere.media.util.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/media/upload", "/api/media/**", "/api/stories/upload", "/api/stories/**")
				.authenticated()
				.requestMatchers("/api/post/**", "/api/media/*", "/api/stories/feed", "/api/stories/*",
						"/api/stories/user/**", "/uploads/**")
				.permitAll()
				.requestMatchers("/swagger-ui/**", "/api/swagger-ui/**", "/api/swagger-ui.html", "/v3/api-docs/**",
						"/v3/api-docs", "/webjars/**", "/swagger-resources/**", "/configuration/**")
				.permitAll().anyRequest().permitAll())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
	}
}