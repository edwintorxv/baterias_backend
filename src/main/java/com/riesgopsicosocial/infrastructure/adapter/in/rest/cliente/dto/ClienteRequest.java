package com.riesgopsicosocial.infrastructure.adapter.in.rest.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClienteRequest (

        @NotNull Long fkIndustria,
        Long fkCiudadMunicipio,
        @NotBlank @Size(max = 50) String nit,
        @NotBlank @Size(max = 300) String nombre,
        @Size(max = 300) String direccion,
        @Size(max = 50) String telefono,
        @Size(max = 200) String nombreContacto,
        @Size(max = 150) String cargoContacto,
        @Size(max = 50) String telefonoContacto

){
}
