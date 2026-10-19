package com.riesgopsicosocial.domain.model.informe.empresa;

import java.time.LocalDate;

/**
 * Persona que la empresa entregó para evaluar (una relación de {@code evaluado_cliente}, vigente o no).
 *
 * @param ciudad           ciudad de trabajo; {@code null} si no está registrada
 * @param ficha            datos generales para describir la población
 * @param fechaVinculacion desde cuándo está en la empresa
 * @param fechaRetiro      {@code null} si sigue vigente
 */
public record PersonaCargada(Long idEvaluadoCliente, String area, CiudadTrabajo ciudad, FichaPersona ficha,
                             LocalDate fechaVinculacion, LocalDate fechaRetiro) {

    /** Persona vigente desde siempre: para pruebas y datos sin fechas. */
    public PersonaCargada(Long idEvaluadoCliente, String area, CiudadTrabajo ciudad, FichaPersona ficha) {
        this(idEvaluadoCliente, area, ciudad, ficha, null, null);
    }

    /** Ya estaba vinculada y aún no se había retirado en esa fecha. */
    public boolean vigenteAl(LocalDate fecha) {
        return (fechaVinculacion == null || !fechaVinculacion.isAfter(fecha))
                && (fechaRetiro == null || fechaRetiro.isAfter(fecha));
    }

    /**
     * Corte para saber quién se esperaba evaluar en el año: el 31 de diciembre, o hoy si el año
     * aún no termina.
     */
    public static LocalDate corteDelAnio(int anio, LocalDate hoy) {
        LocalDate finDeAnio = LocalDate.of(anio, 12, 31);
        return hoy.isBefore(finDeAnio) ? hoy : finDeAnio;
    }

}
