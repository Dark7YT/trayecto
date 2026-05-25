package com.trayecto.notifications.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP broker simple in-memory. Endpoint público {@code /ws} con CORS (los origins se
 * heredan del config global). Auth en CONNECT vía {@link StompAuthChannelInterceptor}.
 * <p>
 * Destinos:
 * - {@code /user/queue/notifications} — push individual (se traduce a /queue/...-{userId}).
 * <p>
 * En producción si crece el tráfico se conecta a RabbitMQ/Redis como broker externo;
 * para Trayecto MVP el broker simple basta (un solo nodo).
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;

    WebSocketConfig(StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*"); // CORS detallado en SecurityConfig
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
