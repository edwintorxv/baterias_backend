package com.riesgopsicosocial.infrastructure.adapter.out.correo;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Brevo simulado con MockRestServiceServer; se llama a enviar() directo, sin @Async. */
class CorreoServiceTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();

    private CorreoService servicio(String apiKey, String remitente) {
        return new CorreoService(builder, apiKey, remitente, "PsicoBatería");
    }

    @Test
    void sinClave_soloLog_noLlamaABrevo() {
        CorreoService correo = servicio("", "remitente@prueba.co");
        assertFalse(correo.enviaDeVerdad());

        correo.enviar("ana@prueba.co", "Asunto", "prueba", Map.of("nombre", "Ana"));
        brevo.verify();
    }

    @Test
    void conClavePeroSinRemitente_soloLog() {
        CorreoService correo = servicio("xkeysib-prueba", " ");
        assertFalse(correo.enviaDeVerdad());

        correo.enviar("ana@prueba.co", "Asunto", "prueba", Map.of("nombre", "Ana"));
        brevo.verify();
    }

    @Test
    void conClave_enviaABrevoConRemitenteDestinatarioYHtml() {
        brevo.expect(requestTo(CorreoService.URL_BREVO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-prueba"))
                .andExpect(jsonPath("$.sender.name").value("PsicoBatería"))
                .andExpect(jsonPath("$.sender.email").value("remitente@prueba.co"))
                .andExpect(jsonPath("$.to[0].email").value("ana@prueba.co"))
                .andExpect(jsonPath("$.subject").value("Bienvenida"))
                .andExpect(jsonPath("$.htmlContent").value(allOf(
                        containsString("Psico<span"), containsString("Hola <b>Ana</b>"),
                        containsString("no responda"))))
                .andRespond(withSuccess("{\"messageId\":\"<1@brevo>\"}", MediaType.APPLICATION_JSON));

        CorreoService correo = servicio("xkeysib-prueba", "remitente@prueba.co");
        assertTrue(correo.enviaDeVerdad());
        correo.enviar("ana@prueba.co", "Bienvenida", "prueba", Map.of("nombre", "Ana"));
        brevo.verify();
    }

    @Test
    void lasVariablesSeEscapan() {
        String html = servicio("", "").armar("Asunto", "prueba", Map.of("nombre", "<script>x</script>"));
        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;"), html);
        assertFalse(html.contains("<script>x"));
    }

    @Test
    void siBrevoFalla_noLanzaExcepcion() {
        brevo.expect(requestTo(CorreoService.URL_BREVO))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Key not found\"}"));

        CorreoService correo = servicio("xkeysib-mala", "remitente@prueba.co");
        assertDoesNotThrow(() -> correo.enviar("ana@prueba.co", "Asunto", "prueba", Map.of("nombre", "Ana")));
        brevo.verify();
    }

    @Test
    void plantillaInexistente_noLanzaExcepcion() {
        CorreoService correo = servicio("xkeysib-prueba", "remitente@prueba.co");
        assertDoesNotThrow(() -> correo.enviar("ana@prueba.co", "Asunto", "no-existe", Map.of()));
        brevo.verify();
    }

}
