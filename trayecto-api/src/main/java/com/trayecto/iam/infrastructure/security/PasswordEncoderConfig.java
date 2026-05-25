package com.trayecto.iam.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class PasswordEncoderConfig {

    /**
     * bcrypt strength 12 ≈ 250ms en hardware moderno. Suficientemente lento para
     * frenar ataques offline, suficientemente rápido para login interactivo.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
