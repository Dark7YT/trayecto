package com.trayecto.iam.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import com.trayecto.iam.infrastructure.ratelimit.RateLimitFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * SecurityFilterChain stateless basado en JWT. OAuth2 Google se cablea cuando llegue
 * task #20 (GoogleOAuth2UserService).
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsProperties corsProperties;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2SuccessHandler;

    SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RateLimitFilter rateLimitFilter,
        CorsProperties corsProperties,
        GoogleOAuth2UserService googleOAuth2UserService,
        OAuth2LoginSuccessHandler oauth2SuccessHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.corsProperties = corsProperties;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(c -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                )
                .referrerPolicy(rp -> rp.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                ))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/login/oauth2/code/**", "/oauth2/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/ws/**").permitAll()  // JWT se valida en ChannelInterceptor
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(ui -> ui.userService(googleOAuth2UserService))
                .successHandler(oauth2SuccessHandler)
            )
            // CRÍTICO: las rutas /api/** son REST/JSON. Cuando el auth falla, deben
            // devolver 401 JSON, NUNCA redirect a /login (HTML). Sin esto, oauth2Login
            // instala su LoginUrlAuthenticationEntryPoint como default y los clientes
            // reciben HTML disfrazado de éxito (200 con login page tras follow-redirect).
            .exceptionHandling(eh -> eh
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    (RequestMatcher) request -> request.getRequestURI().startsWith("/api/")
                )
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(corsProperties.allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Idempotency-Key", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("X-Total-Count"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
