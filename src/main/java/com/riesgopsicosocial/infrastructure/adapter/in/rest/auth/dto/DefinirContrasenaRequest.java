package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BCrypt solo usa los primeros 72 bytes: de ahí el máximo. */
public record DefinirContrasenaRequest(
        @NotBlank @Size(max = 100) String codigo,
        @NotBlank @Size(min = 8, max = 72) String contrasena
) {
}
