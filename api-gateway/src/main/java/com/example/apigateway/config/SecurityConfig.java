package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/oauth2/**", "/.well-known/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/grafana/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/storages/**").hasAuthority("SCOPE_ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/storages/**").hasAuthority("SCOPE_ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/storages/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/resources/**", "/songs/**").authenticated()
                        .pathMatchers(HttpMethod.POST, "/resources/**").hasAuthority("SCOPE_ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/resources/**", "/songs/**").hasAuthority("SCOPE_ROLE_ADMIN")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );

        return http.build();
    }

    private ReactiveJwtAuthenticationConverterAdapter grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new RolesClaimConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    private static class RolesClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> scopes = jwt.getClaimAsStringList("scope");
            if (scopes == null) {
                scopes = jwt.getClaimAsStringList("scp");
            }
            if (scopes == null) {
                return Collections.emptyList();
            }
            return scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());
        }
    }
}
