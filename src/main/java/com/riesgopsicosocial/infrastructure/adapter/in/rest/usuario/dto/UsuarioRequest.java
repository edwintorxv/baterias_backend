package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Actualización sin contraseña: la define el usuario con el enlace (POST /usuarios/{id}/enlace-contrasena). */
public record UsuarioRequest(
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull Rol rol,
        Long idEvaluador,
        Long idCliente,
        Long idEvaluado,
        Boolean activo
) {
}
