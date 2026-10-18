package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String usuario,
        @NotBlank @Size(max = 72) String contrasena
) {
}
