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
 * Envía correos transaccionales vía la API HTTP de Brevo (ex Sendinblue):
 * {@code POST https://api.brevo.com/v3/smtp/email}.
 * <p>
 * Ventaja sobre Resend en tier free:
 * <ul>
 *   <li>Sin dominio propio — basta con verificar tu email personal como "Sender"
 *       desde el dashboard de Brevo (te mandan un link por email).</li>
 *   <li>Permite enviar a CUALQUIER destinatario una vez verificado el sender.</li>
 *   <li>Quota free: 300 emails/día = 9,000/mes (vs 100/día de Resend).</li>
 * </ul>
 * <p>
 * Trade-off: la cuenta Brevo pasa por antifraud review (12–48h en algunos casos).
 * Durante ese período el provider puede dar 401/403 — el código loguea el detalle.
 * <p>
 * Activación: {@code app.mail.provider=brevo}. La API key se lee de {@code BREVO_API_KEY}.
 * El sender ({@code app.mail.from}) debe ser el email verificado en Brevo.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoApiMailSender implements MailDispatchPort {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiMailSender.class);
    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestClient http;
    private final AppMailProperties props;

    public BrevoApiMailSender(AppMailProperties props) {
        this.props = props;
        if (props.brevoApiKey() == null || props.brevoApiKey().isBlank()) {
            throw new IllegalStateException(
                "app.mail.provider=brevo pero BREVO_API_KEY no está configurado. " +
                "Crear una API key en https://app.brevo.com/settings/keys/api.");
        }
        this.http = RestClient.builder()
            .baseUrl(ENDPOINT)
            .defaultHeader("api-key", props.brevoApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
        String tail = props.brevoApiKey().length() > 4
            ? props.brevoApiKey().substring(props.brevoApiKey().length() - 4)
            : "?";
        log.info("BrevoApiMailSender enabled (from={}, key=xkeysib-***{})", props.from(), tail);
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

    /**
     * Brevo espera un body como:
     * <pre>
     * {
     *   "sender":  { "name": "Trayecto", "email": "sender@gmail.com" },
     *   "to":      [{ "email": "to@example.com" }],
     *   "subject": "...",
     *   "htmlContent": "..."
     * }
     * </pre>
     *
     * <p>{@code app.mail.from} puede venir como solo email ({@code foo@gmail.com})
     * o como display + email ({@code Trayecto <foo@gmail.com>}). Acá lo parseamos.
     */
    private void sendHtml(Email to, String subject, String body) {
        Sender sender = parseSender(props.from());
        Map<String, Object> payload = Map.of(
            "sender", Map.of("name", sender.name(), "email", sender.email()),
            "to", List.of(Map.of("email", to.value())),
            "subject", subject,
            "htmlContent", body
        );
        try {
            http.post()
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("Sent '{}' to {} via Brevo", subject, mask(to.value()));
        } catch (RestClientResponseException e) {
            log.warn("Brevo rechazó '{}' a {}: status={} body={}",
                subject, mask(to.value()), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Brevo send failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.warn("Failed to send '{}' to {}: {}", subject, mask(to.value()), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private record Sender(String name, String email) {}

    /**
     * Parsea {@code "Display Name <email@domain>"} o {@code "email@domain"} en
     * un {@link Sender}. Si no hay display name, usa {@code "Trayecto"} por default.
     */
    static Sender parseSender(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("app.mail.from no puede estar vacío");
        }
        String trimmed = raw.trim();
        int ltIdx = trimmed.indexOf('<');
        int gtIdx = trimmed.indexOf('>');
        if (ltIdx > 0 && gtIdx > ltIdx) {
            String name = trimmed.substring(0, ltIdx).trim();
            String email = trimmed.substring(ltIdx + 1, gtIdx).trim();
            if (name.isBlank()) name = "Trayecto";
            return new Sender(name, email);
        }
        return new Sender("Trayecto", trimmed);
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
