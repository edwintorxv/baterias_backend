package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;

/** Sin la contraseña ni su hash. */
public record UsuarioResponse(
        Long id,
        String correo,
        String nombre,
        Rol rol,
        Long idEvaluador,
        Long idCliente,
        Long idEvaluado,
        Boolean activo
) {
}
