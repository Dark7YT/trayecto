package com.trayecto.notifications.infrastructure.websocket;

import com.trayecto.iam.api.AccessTokenValidator;
import com.trayecto.shared.kernel.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lee el JWT del header {@code Authorization: Bearer <token>} en el frame STOMP CONNECT,
 * lo valida y popula el {@code Principal} del canal con un {@link UsernamePasswordAuthenticationToken}
 * cuyo principal es {@link UserId}. Esto habilita {@code convertAndSendToUser(userId, ...)}.
 */
@Component
class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenValidator tokenValidator;

    StompAuthChannelInterceptor(AccessTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            extractToken(accessor)
                .flatMap(tokenValidator::validate)
                .ifPresent(principal -> {
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            principal.userId(), null, List.of()
                        );
                    accessor.setUser(auth);
                    log.debug("STOMP CONNECT authenticated as user {}", principal.userId().asString());
                });
        }
        return message;
    }

    private static Optional<String> extractToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
