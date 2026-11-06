package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EstadoEnlace;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.PropositoEnlace;

import java.time.LocalDateTime;

/**
 * Estado del enlace para la página pública. Si no está vigente solo trae el estado y el mensaje (que
 * remite al administrador); nombre, correo, propósito y vencimiento solo si está vigente.
 */
public record EnlaceContrasenaResponse(
        boolean vigente,
        EstadoEnlace estado,
        String mensaje,
        PropositoEnlace proposito,
        String nombre,
        String correo,
        LocalDateTime vence
) {
}
