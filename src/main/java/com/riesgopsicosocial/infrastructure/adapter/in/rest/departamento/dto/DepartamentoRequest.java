package com.riesgopsicosocial.infrastructure.adapter.in.rest.departamento.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartamentoRequest(String codigoDane,
                                  @NotBlank String nombre
) {

}
