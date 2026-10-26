package cl.duoc.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Configuration
public class SecurityConfig {

    /** Cualquier usuario con sesión (cliente o admin) trae este scope. */
    private static final String CUSTOMER_AUTHORITY = "SCOPE_access_as_user";

    @Value("${spring.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${app.security.issuers}")
    private String issuers;

    @Value("${app.security.audience}")
    private String audience;

    @Value("${app.security.admin-role:Admin}")
    private String adminRole;

    @Bean
    JwtDecoder jwtDecoder() {
        List<String> trusted = Arrays.stream(issuers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new MultiIssuerJwtDecoder(trusted, audience);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            // Modo Desarrollo / Pruebas Rápidas: permite todas las peticiones sin exigir Bearer token
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            String admin = "ROLE_" + adminRole;
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    // Catálogo público: se puede mirar sin iniciar sesión
                    .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                    // Comprar: cualquier usuario con sesión
                    .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority(CUSTOMER_AUTHORITY)
                    .requestMatchers(HttpMethod.POST, "/api/payments/**").hasAuthority(CUSTOMER_AUTHORITY)
                    .requestMatchers(HttpMethod.GET, "/api/orders/mine").hasAuthority(CUSTOMER_AUTHORITY)
                    // Lo demás de pedidos y toda escritura del catálogo: solo administradores
                    .requestMatchers("/api/orders/**").hasAuthority(admin)
                    .requestMatchers("/api/catalog/**").hasAuthority(admin)
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(authenticationConverter())
                ));
        }

        return http.build();
    }

    /** Scopes (scp) + app roles (roles) de Entra como authorities: SCOPE_xxx y ROLE_xxx. */
    private JwtAuthenticationConverter authenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
