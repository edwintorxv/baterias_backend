package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EnlaceContrasenaGenerado;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.PropositoEnlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Correo con el enlace para definir la contraseña. Sale después del commit de la transacción que lo
 * generó (TransactionalEventListener): si algo falla antes, no se envía un enlace que no existe.
 */
@Component
public class CorreoEnlaceContrasena {

    static final String PLANTILLA = "enlace-contrasena";
    static final String RUTA_FRONT = "/definir-contrasena";
    private static final DateTimeFormatter FORMATO_VENCE =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", Locale.forLanguageTag("es-CO"));
    private static final Logger log = LoggerFactory.getLogger(CorreoEnlaceContrasena.class);

    private final CorreoService correoService;
    private final String frontUrl;

    public CorreoEnlaceContrasena(CorreoService correoService, @Value("${app.front.url:}") String frontUrl) {
        this.correoService = correoService;
        this.frontUrl = frontUrl == null ? "" : frontUrl.trim().replaceAll("/+$", "");
        if (this.frontUrl.isEmpty()) {
            log.warn("Falta APP_FRONT_URL: los enlaces de contraseña de los correos quedarán incompletos");
        }
    }

    @TransactionalEventListener
    public void alGenerar(EnlaceContrasenaGenerado evento) {
        boolean invitacion = evento.proposito() == PropositoEnlace.INVITACION;
        String asunto = invitacion ? "Bienvenido a PsicoBatería: defina su contraseña" : "PsicoBatería: defina su nueva contraseña";
        correoService.enviar(evento.correo(), asunto, PLANTILLA, Map.of(
                "nombre", evento.nombre(),
                "correo", evento.correo(),
                "invitacion", invitacion,
                "enlace", enlace(evento.codigo()),
                "vence", FORMATO_VENCE.format(evento.vence())));
    }

    String enlace(String codigo) {
        return UriComponentsBuilder.fromUriString(frontUrl + RUTA_FRONT)
                .queryParam("codigo", codigo)
                .build()
                .toUriString();
    }

}
