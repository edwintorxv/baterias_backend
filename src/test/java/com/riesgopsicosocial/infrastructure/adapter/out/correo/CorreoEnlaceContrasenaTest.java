package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EnlaceContrasenaGenerado;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.PropositoEnlace;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CorreoEnlaceContrasenaTest {

    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 11, 8, 15, 5);

    private final CorreoService correoService = mock(CorreoService.class);
    private final CorreoEnlaceContrasena correo =
            new CorreoEnlaceContrasena(correoService, "https://baterias-front.onrender.com/");

    @SuppressWarnings("unchecked")
    private Map<String, Object> variablesEnviadas(String asunto) {
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(correoService).enviar(eq("ana@prueba.co"), eq(asunto), eq("enlace-contrasena"), variables.capture());
        return variables.getValue();
    }

    @Test
    void invitacion_enlaceAlFrontConElCodigo() {
        correo.alGenerar(new EnlaceContrasenaGenerado("ana@prueba.co", "Ana", PropositoEnlace.INVITACION, "abc-_123", VENCE));

        Map<String, Object> variables = variablesEnviadas("Bienvenido a PsicoBatería: defina su contraseña");
        assertEquals("https://baterias-front.onrender.com/definir-contrasena?codigo=abc-_123", variables.get("enlace"));
        assertEquals(true, variables.get("invitacion"));
        assertTrue(variables.get("vence").toString().startsWith("8 de noviembre de 2026, 3:05"), variables.get("vence").toString());
    }

    @Test
    void restablecer_otroAsunto() {
        correo.alGenerar(new EnlaceContrasenaGenerado("ana@prueba.co", "Ana", PropositoEnlace.RESTABLECER, "abc", VENCE));

        Map<String, Object> variables = variablesEnviadas("PsicoBatería: defina su nueva contraseña");
        assertEquals(false, variables.get("invitacion"));
    }

    @Test
    void plantilla_muestraNombreBotonYVencimiento() {
        CorreoService real = new CorreoService(RestClient.builder(), "", "", "PsicoBatería");
        for (boolean invitacion : new boolean[]{true, false}) {
            String html = real.armar("Asunto", "enlace-contrasena", Map.of(
                    "nombre", "Ana <b>", "correo", "ana@prueba.co", "invitacion", invitacion,
                    "enlace", "https://front/definir-contrasena?codigo=abc", "vence", "8 de noviembre de 2026, 3:05 p. m."));

            assertTrue(html.contains("Ana &lt;b&gt;"), "el nombre va escapado");
            assertTrue(html.contains("href=\"https://front/definir-contrasena?codigo=abc\""));
            assertTrue(html.contains("8 de noviembre de 2026"));
            assertEquals(invitacion, html.contains("Se creó su usuario"));
            assertEquals(!invitacion, html.contains("Su contraseña anterior ya no sirve"));
        }
    }

}
