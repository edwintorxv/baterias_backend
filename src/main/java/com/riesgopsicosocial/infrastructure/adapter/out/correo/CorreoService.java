package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Envío de correos con la API HTTP de Brevo. Cada correo es una plantilla de templates/correo/ que se
 * mete dentro de base.html (encabezado y pie de PsicoBatería).
 * <p>
 * Sin clave de Brevo o sin remitente no se envía nada: el correo queda en el log (modo de desarrollo).
 * Un fallo de Brevo tampoco interrumpe a quien envía: solo queda en el log, sin la clave ni el contenido.
 */
@Component
public class CorreoService {

    static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";
    private static final String BASE = "base";
    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final TemplateEngine templateEngine;

    public CorreoService(RestClient.Builder restClientBuilder,
                         @Value("${app.correo.brevo-api-key:}") String apiKey,
                         @Value("${app.correo.remitente:}") String remitente,
                         @Value("${app.correo.nombre-remitente:PsicoBatería}") String nombreRemitente) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/correo/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
        if (!this.apiKey.isEmpty() && this.remitente.isEmpty()) {
            log.warn("Hay clave de Brevo pero falta APP_CORREO_REMITENTE: los correos solo quedarán en el log");
        }
    }

    /** True si los correos salen por Brevo; false si solo se escriben en el log. */
    public boolean enviaDeVerdad() {
        return !apiKey.isEmpty() && !remitente.isEmpty();
    }

    /**
     * Arma el correo con la plantilla y lo envía en segundo plano (quien llama no espera a Brevo).
     * Las variables se escapan en la plantilla (th:text); solo el cuerpo ya armado entra sin escapar.
     */
    @Async
    public void enviar(String para, String asunto, String plantilla, Map<String, Object> variables) {
        try {
            String html = armar(asunto, plantilla, variables);
            if (!enviaDeVerdad()) {
                log.info("Correo sin enviar (modo log, falta BREVO_API_KEY o remitente) a {} | asunto: {} | variables: {}",
                        para, asunto, variables);
                return;
            }
            restClient.post()
                    .uri(URL_BREVO)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("name", nombreRemitente, "email", remitente),
                            "to", List.of(Map.of("email", para)),
                            "subject", asunto,
                            "htmlContent", html))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Correo enviado a {} | asunto: {}", para, asunto);
        } catch (RestClientResponseException e) {
            log.warn("Brevo rechazó el correo a {} | asunto: {} | HTTP {}: {}", para, asunto,
                    e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo a {} | asunto: {} | {}", para, asunto, e.toString());
        }
    }

    String armar(String asunto, String plantilla, Map<String, Object> variables) {
        Context contexto = new Context();
        contexto.setVariables(variables);
        String cuerpo = templateEngine.process(plantilla, contexto);

        Context base = new Context();
        base.setVariable("asunto", asunto);
        base.setVariable("cuerpo", cuerpo);
        return templateEngine.process(BASE, base);
    }

}
