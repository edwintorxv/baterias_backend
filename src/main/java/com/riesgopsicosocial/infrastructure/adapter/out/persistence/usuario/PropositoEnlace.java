package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

/** Para qué se generó un enlace de contraseña: cambia el texto del correo, no las reglas. */
public enum PropositoEnlace {
    /** Usuario nuevo que aún no ha definido su contraseña. */
    INVITACION,
    /** Se le olvidó: llamó por teléfono y el administrador generó el enlace. */
    RESTABLECER
}
