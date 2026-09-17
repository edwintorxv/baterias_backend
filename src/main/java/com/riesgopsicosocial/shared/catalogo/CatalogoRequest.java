package com.riesgopsicosocial.shared.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
        String nombre
) {

}
