package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.shared.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LimiteIntentosLoginTest {

    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    /** Reloj que avanza a mano. */
    private static class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-10-18T12:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final Reloj reloj = new Reloj();
    private final LimiteIntentosLogin limite = new LimiteIntentosLogin(5, 20, BLOQUEO, reloj);

    private void fallar(String usuario, String ip, int veces) {
        for (int i = 0; i < veces; i++) {
            limite.registrarFallo(usuario, ip);
        }
    }

    @Test
    void bloqueaAlLlegarAlMaximoPorUsuario() {
        fallar("psicologa", "10.0.0.1", 4);
        assertDoesNotThrow(() -> limite.verificar("psicologa", "10.0.0.1"));

        fallar("psicologa", "10.0.0.1", 1);
        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> limite.verificar("psicologa", "10.0.0.2"));
        assertEquals(900, ex.segundos());
        assertEquals("Demasiados intentos fallidos. Intente de nuevo en 15 minutos", ex.getMessage());
    }

    @Test
    void elUsuarioSeNormaliza() {
        fallar(" Psicologa ", "10.0.0.1", 5);
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("PSICOLOGA", "10.0.0.9"));
    }

    @Test
    void elBloqueoVenceDesdeElUltimoFallo() {
        fallar("psicologa", "10.0.0.1", 5);
        reloj.avanzar(Duration.ofMinutes(14).plusSeconds(30));
        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> limite.verificar("psicologa", "10.0.0.1"));
        assertEquals(30, ex.segundos());
        assertEquals("Demasiados intentos fallidos. Intente de nuevo en 1 minuto", ex.getMessage());

        reloj.avanzar(Duration.ofSeconds(30));
        assertDoesNotThrow(() -> limite.verificar("psicologa", "10.0.0.1"));
    }

    @Test
    void losFallosViejosSeOlvidan() {
        fallar("psicologa", "10.0.0.1", 4);
        reloj.avanzar(BLOQUEO);
        fallar("psicologa", "10.0.0.1", 4);
        assertDoesNotThrow(() -> limite.verificar("psicologa", "10.0.0.1"));
    }

    @Test
    void elExitoReiniciaElUsuarioPeroNoLaIp() {
        fallar("psicologa", "10.0.0.1", 4);
        limite.registrarExito("psicologa");
        fallar("psicologa", "10.0.0.1", 4);
        assertDoesNotThrow(() -> limite.verificar("psicologa", "10.0.0.1"));

        for (int i = 0; i < 12; i++) {
            limite.registrarFallo("otro" + i, "10.0.0.1");
        }
        limite.registrarExito("otro0");
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("nuevo", "10.0.0.1"));
        assertDoesNotThrow(() -> limite.verificar("nuevo", "10.0.0.2"));
    }

    @Test
    void bloqueaPorIpAunqueCambieDeUsuario() {
        for (int i = 0; i < 19; i++) {
            limite.registrarFallo("usuario" + i, "10.0.0.1");
        }
        assertDoesNotThrow(() -> limite.verificar("cualquiera", "10.0.0.1"));
        limite.registrarFallo("usuario19", "10.0.0.1");
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("cualquiera", "10.0.0.1"));
    }

}
