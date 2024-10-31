package com.mstfcmrl.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws Exception {
        http
            .authorizeExchange(exchanges ->
                exchanges
                    .pathMatchers("/keycloak/**", "/admin/**").permitAll() // Allow unauthenticated access to Keycloak routes
                    .anyExchange().authenticated() // All routes require authentication
            )
            // Enable OAuth2 login for UI login
            .oauth2Login(withDefaults())
            // Enable JWT authentication for API requests
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(withDefaults()));

        return http.build();
    }
}
