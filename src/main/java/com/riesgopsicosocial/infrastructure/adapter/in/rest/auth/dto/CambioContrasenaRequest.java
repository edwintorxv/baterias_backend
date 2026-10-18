package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioContrasenaRequest(
        @NotBlank @Size(max = 72) String actual,
        @NotBlank @Size(min = 8, max = 72) String nueva
) {
}
