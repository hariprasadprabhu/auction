package com.bid.auction.config;

import com.bid.auction.security.JwtAuthFilter;
import com.bid.auction.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,https://auctiondeck-api-production.up.railway.app,https://auctiondeck-web.railway.app,https://auction-web.railway.app,https://auctiondeck-web.onrender.com,https://auction-web.onrender.com,https://auctiondeck.railway.app,https://auctiondeck.onrender.com,https://auction-ui-production-397c.up.railway.app}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Health check (for load balancers and health monitoring)
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Allow CORS preflight OPTIONS requests
                        // Swagger UI / OpenAPI docs
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**"
                        ).permitAll()
                        // Public auth endpoints (both /auth/** and /api/auth/** due to context-path)
                        .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                        // Public tournament details (for registration page)
                        .requestMatchers(HttpMethod.GET, "/tournaments/*/public", "/api/tournaments/*/public").permitAll()
                        // Public player self-registration
                        .requestMatchers(HttpMethod.POST, "/players/register/**", "/api/players/register/**").permitAll()
                        // Public image endpoints (Angular <img [src]="...">)
                        .requestMatchers(HttpMethod.GET, "/tournaments/*/logo", "/api/tournaments/*/logo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/teams/*/logo", "/api/teams/*/logo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auction-players/*/photo", "/api/auction-players/*/photo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/players/*/photo", "/api/players/*/photo").permitAll()
                        // Everything else requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Parse comma-separated origins from environment variable
        String[] origins = allowedOrigins.split(",");
        // Trim whitespace from each origin
        List<String> trimmedOrigins = Arrays.stream(origins)
                .map(String::trim)
                .toList();
        
        System.out.println("🔐 CORS Configuration - Allowed Origins: " + trimmedOrigins);
        
        config.setAllowedOrigins(trimmedOrigins);
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // When allowCredentials is true, cannot use wildcard - must list explicit headers
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-CSRF-Token"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
