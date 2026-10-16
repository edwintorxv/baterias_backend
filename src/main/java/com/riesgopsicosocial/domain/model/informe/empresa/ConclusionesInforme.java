package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;

import java.util.ArrayList;
import java.util.List;

/**
 * Conclusiones y plan de acción que el profesional escribe para el informe de empresa de un año.
 * Cualquier texto puede faltar ({@code null}) mientras se diligencia.
 *
 * @param profesional profesional responsable; {@code null} si no se ha asignado
 */
public record ConclusionesInforme(String conclusiones, String accionesCortoPlazo, String accionesMedianoPlazo,
                                  String accionesLargoPlazo, DatosEvaluador profesional) {

    public static final ConclusionesInforme VACIAS = new ConclusionesInforme(null, null, null, null, null);

    /**
     * Lo que falta para poder descargar el informe: las conclusiones y las tres acciones del plan
     * son obligatorias (decisión del usuario, 2026-10-15). Vacío = completo.
     */
    public List<String> faltantes() {
        List<String> faltantes = new ArrayList<>();
        agregarSiFalta(faltantes, conclusiones, "conclusiones del profesional");
        agregarSiFalta(faltantes, accionesCortoPlazo, "acciones a corto plazo");
        agregarSiFalta(faltantes, accionesMedianoPlazo, "acciones a mediano plazo");
        agregarSiFalta(faltantes, accionesLargoPlazo, "acciones a largo plazo");
        return faltantes;
    }

    private static void agregarSiFalta(List<String> faltantes, String texto, String nombre) {
        if (texto == null || texto.isBlank()) {
            faltantes.add(nombre);
        }
    }

}
