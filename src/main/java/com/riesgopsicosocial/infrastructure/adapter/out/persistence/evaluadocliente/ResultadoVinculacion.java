package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

/** Qué pasó (o qué pasaría) al vincular una persona a un cliente. */
public enum ResultadoVinculacion {

    /** El documento no existía: se crea la persona y su relación. */
    NUEVA,
    /** La persona existía sin vínculo activo: se crea la relación. */
    VINCULA,
    /** Ya estaba activa en este cliente: se actualiza la foto de esa relación. */
    ACTUALIZA,
    /** Estaba activa en otra empresa: esa relación se cierra y se crea la nueva. */
    CAMBIA_EMPRESA

}
