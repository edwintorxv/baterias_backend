package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.ContrasenaCambiada;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CorreoContrasenaCambiadaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 6, 9, 5);

    private final CorreoService correoService = mock(CorreoService.class);
    private final CorreoContrasenaCambiada correo = new CorreoContrasenaCambiada(correoService);

    @SuppressWarnings("unchecked")
    private Map<String, Object> variablesEnviadas(String asunto) {
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(correoService).enviar(eq("ana@prueba.co"), eq(asunto), eq("contrasena-cambiada"), variables.capture());
        return variables.getValue();
    }

    @Test
    void cambio_asuntoYFechaSinContrasena() {
        correo.alCambiar(new ContrasenaCambiada("ana@prueba.co", "Ana", FECHA, false));

        Map<String, Object> variables = variablesEnviadas("PsicoBatería: su contraseña cambió");
        assertTrue(variables.get("fecha").toString().startsWith("6 de noviembre de 2026, 9:05"), variables.get("fecha").toString());
        assertEquals(Map.of("nombre", "Ana", "correo", "ana@prueba.co", "primeraVez", false,
                "fecha", variables.get("fecha")), variables, "solo estas variables: nunca la contraseña");
    }

    @Test
    void primeraVez_otroAsunto() {
        correo.alCambiar(new ContrasenaCambiada("ana@prueba.co", "Ana", FECHA, true));

        assertEquals(true, variablesEnviadas("PsicoBatería: se definió su contraseña").get("primeraVez"));
    }

    @Test
    void plantilla_textoSegunElCasoYAvisoDeLlamar() {
        CorreoService real = new CorreoService(RestClient.builder(), "", "", "PsicoBatería");
        for (boolean primeraVez : new boolean[]{true, false}) {
            String html = real.armar("Asunto", "contrasena-cambiada", Map.of("nombre", "Ana", "correo", "ana@prueba.co",
                    "primeraVez", primeraVez, "fecha", "6 de noviembre de 2026, 9:05 a. m."));

            assertEquals(primeraVez, html.contains("Se definió la contraseña"));
            assertEquals(!primeraVez, html.contains("Se cambió la contraseña"));
            assertTrue(html.contains("6 de noviembre de 2026, 9:05 a. m."));
            assertTrue(html.contains("¿No fue usted?"));
            assertEquals(!primeraVez, html.contains("se cerraron"), "en la invitación no había sesiones");
        }
    }

}
