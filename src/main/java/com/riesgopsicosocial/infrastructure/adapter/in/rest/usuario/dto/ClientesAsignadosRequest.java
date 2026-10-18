package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Lista completa de clientes del psicólogo: reemplaza la anterior (vacía = ninguno). */
public record ClientesAsignadosRequest(
        @NotNull List<@NotNull Long> idsCliente
) {
}
