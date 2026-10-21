package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Según el rol va un solo vínculo: idEvaluador (PSICOLOGO), idCliente (CLIENTE) o idEvaluado
 * (EVALUADO); ADMIN sin vínculo. BCrypt solo usa los primeros 72 bytes: de ahí el máximo.
 */
public record UsuarioCrearRequest(
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(min = 8, max = 72) String contrasena,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull Rol rol,
        Long idEvaluador,
        Long idCliente,
        Long idEvaluado,
        Boolean activo
) {
}
