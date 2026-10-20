package cl.duoc.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            ));

        if (!securityEnabled) {
            // Modo Desarrollo / Pruebas Rápidas: permite todas las peticiones sin exigir Bearer token
            http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        } else {
            // Modo Producción: exige token OAuth2 emitido por Microsoft Entra ID
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/orders/**", "/api/orders").hasAuthority("SCOPE_access_as_user")
                    .requestMatchers("/api/catalog/**", "/api/catalog").hasAuthority("SCOPE_access_as_user")
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth ->
                    oauth.jwt(Customizer.withDefaults())
                );
        }

        return http.build();
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