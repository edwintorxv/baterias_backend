package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

/**
 * Estado de un enlace de contraseña. Si no está vigente, el mensaje remite al administrador por
 * teléfono: el sistema no ofrece pedir otro enlace.
 */
public enum EstadoEnlace {
    VIGENTE(null),
    VENCIDO("El enlace venció. Comuníquese por teléfono con el administrador para que le genere uno nuevo."),
    USADO("El enlace ya se usó. Si necesita definir otra contraseña, comuníquese por teléfono con el administrador."),
    ANULADO("El enlace ya no sirve porque se generó uno más reciente: use el último que recibió o comuníquese por teléfono con el administrador."),
    /** No existe o es de un usuario inactivo: no se dice cuál. */
    INVALIDO("El enlace no es válido. Comuníquese por teléfono con el administrador.");

    private final String mensaje;

    EstadoEnlace(String mensaje) {
        this.mensaje = mensaje;
    }

    public String mensaje() {
        return mensaje;
    }
}
