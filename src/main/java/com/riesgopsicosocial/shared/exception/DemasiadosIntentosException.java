package com.riesgopsicosocial.shared.exception;

import java.time.Duration;

/** Inicio de sesión bloqueado por demasiados fallos (429); {@code espera} es lo que falta para reintentar. */
public class DemasiadosIntentosException extends RuntimeException {

    private final Duration espera;

    public DemasiadosIntentosException(Duration espera) {
        super("Demasiados intentos fallidos. Intente de nuevo en " + minutos(espera)
                + (minutos(espera) == 1 ? " minuto" : " minutos"));
        this.espera = espera;
    }

    /** Segundos para el encabezado Retry-After (redondeados hacia arriba). */
    public long segundos() {
        return (espera.toMillis() + 999) / 1000;
    }

    private static long minutos(Duration espera) {
        return Math.max(1, (espera.getSeconds() + 59) / 60);
    }

}
