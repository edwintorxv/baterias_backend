package com.riesgopsicosocial.domain.model.informe;

/**
 * @param sexo            nombre del catálogo; {@code null} si no está registrado
 * @param anioNacimiento  {@code null} si no está registrado
 */
public record DatosEvaluado(Long id, String numeroIdentificacion, String nombre, String apellido,
                            String sexo, Integer anioNacimiento) {

    /**
     * Edad cumplida en el año indicado. Solo se conoce el año de nacimiento, así que puede
     * ser un año mayor que la real si aún no ha cumplido años. {@code null} si falta el dato.
     */
    public Integer edadEn(int anio) {
        return anioNacimiento == null ? null : anio - anioNacimiento;
    }

}
