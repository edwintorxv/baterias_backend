package com.riesgopsicosocial.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Sin secreto, o con uno corto, la app no arranca: nunca hay uno por defecto fuera del perfil dev. */
class JwtConfigTest {

    @Test
    void sinSecretoOCorto_noArranca() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig(""));
        assertThrows(IllegalStateException.class, () -> new JwtConfig("demasiado-corto"));
        assertDoesNotThrow(() -> new JwtConfig("x".repeat(32)));
    }

}
