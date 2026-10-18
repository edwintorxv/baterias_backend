package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;

/** Quién inició sesión, para que el front sepa qué mostrar. */
public record UsuarioSesionResponse(
        Long id,
        String usuario,
        String nombre,
        Rol rol,
        Long idEvaluador,
        Long idCliente
) {
}
