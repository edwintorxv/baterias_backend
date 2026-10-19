package com.riesgopsicosocial.domain.model.informe.empresa;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PersonaCargadaTest {

    private static PersonaCargada persona(String vinculacion, String retiro) {
        return new PersonaCargada(1L, "A", null, FichaPersona.SIN_DATOS,
                vinculacion == null ? null : LocalDate.parse(vinculacion),
                retiro == null ? null : LocalDate.parse(retiro));
    }

    @Test
    void vigente_desdeElDiaDeVinculacionHastaAntesDelRetiro() {
        PersonaCargada persona = persona("2025-02-01", "2025-06-30");

        assertFalse(persona.vigenteAl(LocalDate.parse("2025-01-31")));
        assertTrue(persona.vigenteAl(LocalDate.parse("2025-02-01")));
        assertTrue(persona.vigenteAl(LocalDate.parse("2025-06-29")));
        assertFalse(persona.vigenteAl(LocalDate.parse("2025-06-30")));
    }

    @Test
    void sinFechas_siempreVigente() {
        assertTrue(persona(null, null).vigenteAl(LocalDate.parse("2020-01-01")));
    }

    @Test
    void corte_finDeAnioOHoySiElAnioNoHaTerminado() {
        LocalDate hoy = LocalDate.parse("2026-10-19");

        assertEquals(LocalDate.parse("2025-12-31"), PersonaCargada.corteDelAnio(2025, hoy));
        assertEquals(hoy, PersonaCargada.corteDelAnio(2026, hoy));
        assertEquals(hoy, PersonaCargada.corteDelAnio(2027, hoy));
    }

}
