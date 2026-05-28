package com.trayecto.iam.infrastructure.security;

import com.trayecto.iam.domain.DisplayName;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Custom OAuth2UserService que mapea el usuario de Google a un {@link User} local.
 * <p>
 * Reglas:
 * - Si no existe usuario con ese email: lo crea como GOOGLE provider, status=ACTIVE
 *   (Google ya verificó el email).
 * - Si existe con provider=LOCAL: lo eleva a BOTH (linking de cuenta).
 * - Si existe con provider=GOOGLE pero distinto sub: rechazo (defensa).
 * - Si está DEACTIVATED: rechazo.
 * <p>
 * El AuthenticationSuccessHandler luego emite un JWT + cookie refresh como en login local
 * y redirige al frontend. Ese handler se construye en SecurityConfig.
 */
@Component
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2UserService.class);
    private static final String USER_ID_ATTRIBUTE = "trayecto_user_id";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        log.info("GoogleOAuth2UserService.loadUser invoked");
        OAuth2User oauthUser = delegate.loadUser(request);
        Map<String, Object> attrs = oauthUser.getAttributes();

        String googleSub = (String) attrs.get("sub");
        String emailValue = (String) attrs.get("email");
        Object emailVerified = attrs.get("email_verified");
        String name = (String) attrs.getOrDefault("name", emailValue);
        log.info("Google OAuth loadUser: email={}, verified={}, sub={}",
            emailValue, emailVerified, googleSub != null ? googleSub.substring(0, Math.min(8, googleSub.length())) + "***" : "null");

        if (googleSub == null || emailValue == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info",
                "Google user info missing sub or email", null));
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified",
                "Google account email is not verified", null));
        }

        Email email = Email.of(emailValue);
        DisplayName displayName = new DisplayName(name);

        User user = userRepository.findByGoogleSubject(googleSub).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            user = User.registerWithGoogle(
                email, googleSub, displayName,
                Locale.forLanguageTag("es-PE"), ZoneId.of("America/Lima")
            );
            user = userRepository.save(user);
            log.info("Registered new user via Google OAuth: {}", emailValue);
        } else {
            if (user.status() == com.trayecto.iam.domain.UserStatus.DEACTIVATED) {
                throw new OAuth2AuthenticationException(new OAuth2Error("account_deactivated",
                    "Account is deactivated", null));
            }
            user.linkGoogleAccount(googleSub);
            user = userRepository.save(user);
        }

        // Devolvemos un OAuth2User con un atributo extra para que el SuccessHandler
        // recupere el UserId sin volver a consultar la BD.
        Map<String, Object> enrichedAttrs = new HashMap<>(attrs);
        String userIdStr = user.id() != null ? user.id().asString() : null;
        if (userIdStr == null) {
            log.error("User id is null after save! email={}, user.toString={}", emailValue, user);
        }
        enrichedAttrs.put(USER_ID_ATTRIBUTE, userIdStr);
        log.info("Google OAuth loadUser returning enriched user with id={}", userIdStr);
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            enrichedAttrs,
            USER_ID_ATTRIBUTE
        );
    }

    public static String extractUserId(OAuth2User user) {
        return (String) user.getAttributes().get(USER_ID_ATTRIBUTE);
    }
}
