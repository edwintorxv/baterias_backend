package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.ContrasenaCambiada;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Aviso de que la contraseña se definió o cambió (sin la contraseña): si no fue el dueño del correo, se entera
 * y llama al administrador. Sale después del commit, como el correo del enlace.
 */
@Component
public class CorreoContrasenaCambiada {

    static final String PLANTILLA = "contrasena-cambiada";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", Locale.forLanguageTag("es-CO"));

    private final CorreoService correoService;

    public CorreoContrasenaCambiada(CorreoService correoService) {
        this.correoService = correoService;
    }

    @TransactionalEventListener
    public void alCambiar(ContrasenaCambiada evento) {
        String asunto = evento.primeraVez() ? "PsicoBatería: se definió su contraseña" : "PsicoBatería: su contraseña cambió";
        correoService.enviar(evento.correo(), asunto, PLANTILLA, Map.of(
                "nombre", evento.nombre(),
                "correo", evento.correo(),
                "primeraVez", evento.primeraVez(),
                "fecha", FORMATO_FECHA.format(evento.fecha())));
    }

}
