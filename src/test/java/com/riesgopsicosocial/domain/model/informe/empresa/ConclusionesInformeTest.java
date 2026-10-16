package com.riesgopsicosocial.domain.model.informe.empresa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConclusionesInformeTest {

    @Test
    void completas_sinFaltantes() {
        assertTrue(new ConclusionesInforme("c", "corto", "mediano", "largo", null).faltantes().isEmpty(),
                "el profesional no es obligatorio para descargar");
    }

    @Test
    void faltantes_listaLasPartesVaciasOEnBlanco() {
        assertEquals(List.of("conclusiones del profesional", "acciones a mediano plazo"),
                new ConclusionesInforme("  ", "corto", null, "largo", null).faltantes());
        assertEquals(4, ConclusionesInforme.VACIAS.faltantes().size());
    }

}
