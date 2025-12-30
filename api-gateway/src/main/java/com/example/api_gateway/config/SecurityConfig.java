package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:8080/");

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/fallbackRoute", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Правила для Product Service
                        .requestMatchers(HttpMethod.GET, "/api/product/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/product/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/product/**").hasRole("ADMIN")

                        // Правила для Inventory Service
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")

                        // Правила для Order Service
                        .requestMatchers("/api/order/**").hasAnyRole("USER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}