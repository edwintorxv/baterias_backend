package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

/** Si el usuario ya puede ingresar o qué le falta, para el listado del administrador. */
public enum EstadoContrasena {
    /** Ya tiene contraseña. */
    DEFINIDA,
    /** Sin contraseña, con un enlace vigente (invitación o restablecer) que aún no usa. */
    PENDIENTE,
    /** Sin contraseña y sin enlace vigente (vencido, anulado o nunca generado): hay que generarle otro. */
    SIN_ENLACE
}
