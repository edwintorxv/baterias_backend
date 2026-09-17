package com.riesgopsicosocial.infrastructure.adapter.in.rest.cliente.dto;

public record ClienteResponse(

        Long id,
        Long fkIndustria,
        Long fkCiudadMunicipio,
        String nit,
        String nombre,
        String direccion,
        String telefono,
        String nombreContacto,
        String cargoContacto,
        String telefonoContacto

) {
}
