package com.trayecto.notifications.infrastructure.mail;

import com.trayecto.notifications.api.MailDispatchPort;
import com.trayecto.shared.kernel.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Envía correos transaccionales vía la API HTTP de Resend (https://resend.com).
 * <p>
 * Existe porque <strong>Render free tier bloquea los puertos SMTP outbound</strong>
 * (25, 465, 587) para evitar abuso. JavaMail timeout-ea conectando a smtp.gmail.com.
 * Resend ofrece API HTTP (POST a /emails) que pasa el bloqueo y nos da 100 emails/día
 * gratis en tier free — más que suficiente para portfolio.
 * <p>
 * Activación: {@code app.mail.provider=resend} en el {@code application.yml} del
 * profile prod (o env var {@code APP_MAIL_PROVIDER=resend}). La API key se lee
 * de {@code RESEND_API_KEY}.
 * <p>
 * El remitente ({@code app.mail.from}) debe usar un dominio verificado en Resend.
 * Por default Resend acepta {@code onboarding@resend.dev} para uso de desarrollo
 * pero solo envía al email verificado en la cuenta. Para enviar a cualquier dirección,
 * verificar un dominio propio.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailSender implements MailDispatchPort {

    private static final Logger log = LoggerFactory.getLogger(ResendMailSender.class);
    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final RestClient http;
    private final AppMailProperties props;

    public ResendMailSender(AppMailProperties props) {
        this.props = props;
        if (props.resendApiKey() == null || props.resendApiKey().isBlank()) {
            throw new IllegalStateException(
                "app.mail.provider=resend pero RESEND_API_KEY no está configurado. " +
                "Crear una API key en https://resend.com → Settings → API Keys.");
        }
        this.http = RestClient.builder()
            .baseUrl(ENDPOINT)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.resendApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
        log.info("ResendMailSender enabled (from={}, key=re_***{})",
            props.from(),
            props.resendApiKey().length() > 4
                ? props.resendApiKey().substring(props.resendApiKey().length() - 4)
                : "?");
    }

    @Override
    public void sendEmailVerification(Email to, String displayName, String tokenRaw) {
        String link = buildLink("/verify-email?token=" + tokenRaw);
        String subject = "Verifica tu correo en Trayecto";
        String body = """
            <!doctype html>
            <html lang="es"><body style="font-family:system-ui,sans-serif;color:#0a0a0a">
              <h2 style="margin-top:0">Hola %s,</h2>
              <p>Gracias por registrarte en <strong>Trayecto</strong>. Confirma tu correo para activar tu cuenta:</p>
              <p style="margin:32px 0">
                <a href="%s" style="background:#0a0a0a;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none">
                  Verificar correo
                </a>
              </p>
              <p style="font-size:14px;color:#666">Si el botón no funciona, copia este enlace en tu navegador:<br>
                <a href="%s">%s</a>
              </p>
              <p style="font-size:14px;color:#666">El enlace expira en 24 horas.<br>
                Si no creaste esta cuenta, ignora este correo.
              </p>
            </body></html>
            """.formatted(displayName, link, link, link);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendPasswordReset(Email to, String tokenRaw) {
        String link = buildLink("/reset-password?token=" + tokenRaw);
        String subject = "Restablece tu contraseña de Trayecto";
        String body = """
            <!doctype html>
            <html lang="es"><body style="font-family:system-ui,sans-serif;color:#0a0a0a">
              <h2 style="margin-top:0">Restablecer contraseña</h2>
              <p>Recibimos una solicitud para restablecer tu contraseña. Haz click en el botón:</p>
              <p style="margin:32px 0">
                <a href="%s" style="background:#0a0a0a;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none">
                  Restablecer contraseña
                </a>
              </p>
              <p style="font-size:14px;color:#666">El enlace expira en 1 hora.<br>
                Si no fuiste tú, ignora este correo — tu contraseña actual sigue activa.
              </p>
            </body></html>
            """.formatted(link);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendShareInvitation(Email to, String ownerName, String tokenRaw) {
        String link = buildLink("/shared/accept?token=" + tokenRaw);
        String subject = ownerName + " quiere compartir sus viajes contigo en Trayecto";
        String body = """
            <!doctype html>
            <html lang="es"><body style="font-family:system-ui,sans-serif;color:#0a0a0a">
              <h2 style="margin-top:0">Hola,</h2>
              <p><strong>%s</strong> quiere compartir contigo el detalle de sus viajes en Trayecto
                (kilómetros recorridos, costos calculados, etc.).</p>
              <p style="margin:32px 0">
                <a href="%s" style="background:#0a0a0a;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none">
                  Ver invitación
                </a>
              </p>
              <p style="font-size:14px;color:#666">Si aún no tienes cuenta, regístrate con este mismo correo
                para poder aceptar la invitación.</p>
            </body></html>
            """.formatted(ownerName, link);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendShareAccepted(Email ownerEmail, String granteeName, String granteeEmail) {
        String link = buildLink("/share");
        String subject = granteeName + " aceptó ver tus viajes en Trayecto";
        String body = """
            <!doctype html>
            <html lang="es"><body style="font-family:system-ui,sans-serif;color:#0a0a0a">
              <h2 style="margin-top:0">¡Buena noticia!</h2>
              <p><strong>%s</strong> (%s) aceptó tu invitación y ya puede ver tus viajes
                cerrados desde su cuenta.</p>
              <p style="margin:32px 0">
                <a href="%s" style="background:#0a0a0a;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none">
                  Ver acceso compartido
                </a>
              </p>
            </body></html>
            """.formatted(granteeName, granteeEmail, link);
        sendHtml(ownerEmail, subject, body);
    }

    @Override
    public void sendCommentNotification(Email to, String authorName, String preview, String tripId) {
        String link = buildLink("/trips/" + tripId);
        String subject = "Nuevo comentario de " + authorName + " en tu viaje";
        String safePreview = preview.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String body = """
            <!doctype html>
            <html lang="es"><body style="font-family:system-ui,sans-serif;color:#0a0a0a">
              <h2 style="margin-top:0">Nuevo comentario</h2>
              <p><strong>%s</strong> comentó en uno de tus viajes:</p>
              <blockquote style="border-left:3px solid #ccc;padding:8px 16px;margin:16px 0;color:#444">%s</blockquote>
              <p style="margin:32px 0">
                <a href="%s" style="background:#0a0a0a;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none">
                  Ver viaje y responder
                </a>
              </p>
            </body></html>
            """.formatted(authorName, safePreview, link);
        sendHtml(to, subject, body);
    }

    private String buildLink(String path) {
        String base = props.appUrl().endsWith("/")
            ? props.appUrl().substring(0, props.appUrl().length() - 1)
            : props.appUrl();
        return URI.create(base + path).toString();
    }

    private void sendHtml(Email to, String subject, String body) {
        Map<String, Object> payload = Map.of(
            "from", props.from(),
            "to", List.of(to.value()),
            "subject", subject,
            "html", body
        );
        try {
            http.post()
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("Sent '{}' to {} via Resend", subject, mask(to.value()));
        } catch (RestClientResponseException e) {
            // 4xx/5xx de Resend — incluye el body en el log para diagnóstico
            log.warn("Resend rechazó '{}' a {}: status={} body={}",
                subject, mask(to.value()), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Resend send failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.warn("Failed to send '{}' to {}: {}", subject, mask(to.value()), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
