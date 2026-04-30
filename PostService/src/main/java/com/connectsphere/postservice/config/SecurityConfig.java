package com.connectsphere.postservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.connectsphere.postservice.util.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtFilter;

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers("/swagger-ui/**", "/api/swagger-ui/**", "/api/swagger-ui.html",
				"/v3/api-docs/**", "/v3/api-docs", "/webjars/**", "/swagger-resources/**", "/configuration/**");
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/posts/search/**").permitAll()
						.requestMatchers("/api/posts/search").permitAll()
						 .requestMatchers("/api/posts/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/posts/{postId}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/posts/user/{userId}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/posts/user/{userId}/count").permitAll().anyRequest()
						.authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}