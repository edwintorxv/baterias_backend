package com.riesgopsicosocial.domain.model.informe;

import java.time.LocalDate;

/**
 * Psicólogo que firma el informe. Los formatos modelo de la batería exigen estos datos
 * ("todo informe que carezca de estos datos no será válido"). La imagen de la firma no
 * viaja aquí: {@code tieneFirma} indica si existe.
 */
public record DatosEvaluador(
        Long id,
        String numeroIdentificacion,
        String nombre,
        String profesion,
        String posgrado,
        String tarjetaProfesional,
        String licenciaSaludOcupacional,
        LocalDate fechaExpedicionLicencia,
        boolean tieneFirma
) {
}
