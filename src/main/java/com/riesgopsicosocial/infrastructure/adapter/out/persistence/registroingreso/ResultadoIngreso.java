package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

public enum ResultadoIngreso {
    EXITOSO,
    /** Contraseña errada, usuario inexistente o inactivo (no se distingue, igual que en la respuesta). */
    FALLIDO,
    /** Primer rechazo de cada bloqueo por fuerza bruta (429). */
    BLOQUEADO
}
