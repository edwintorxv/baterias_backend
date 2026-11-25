package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EstadoContrasena;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;

import java.time.LocalDateTime;

/**
 * Sin la contraseña ni su hash. {@code estadoContrasena} dice si ya puede ingresar (DEFINIDA), si tiene un enlace
 * sin usar (PENDIENTE, con {@code venceEnlace}) o si hay que generarle otro (SIN_ENLACE).
 */
public record UsuarioResponse(
        Long id,
        String correo,
        String nombre,
        Rol rol,
        Long idEvaluador,
        Long idCliente,
        Long idEvaluado,
        Boolean activo,
        EstadoContrasena estadoContrasena,
        LocalDateTime venceEnlace
) {
}
