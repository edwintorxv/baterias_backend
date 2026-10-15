package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Datos generales de una persona para describir la población. Cualquier campo puede ser
 * {@code null} (no registrado).
 *
 * @param tipoCargo    de la relación con la empresa (valor actual de {@code evaluado_cliente})
 * @param tipoContrato de la relación con la empresa (valor actual de {@code evaluado_cliente})
 */
public record FichaPersona(Categoria sexo, Integer anioNacimiento, Categoria nivelAcademico, Categoria tipoCargo,
                           Categoria tipoContrato) {

    public static final FichaPersona SIN_DATOS = new FichaPersona(null, null, null, null, null);

}
