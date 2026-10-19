package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogosCargaTest {

    private final CatalogosCarga catalogos = ArchivoDePrueba.catalogos();

    @Test
    void sinImportarMayusculasTildesNiEspacios() {
        assertEquals(7L, catalogos.buscar(ListaPlantilla.NIVEL_ACADEMICO, "TECNICO/Tecnologico   completo").id());
    }

    @Test
    void ciudad_conDepartamentoConCualquierGuionOSoloElNombreSiEsUnico() {
        assertEquals(149L, catalogos.buscar(ListaPlantilla.CIUDAD, "Bogota, D.C. - Bogotá").id());
        assertEquals(1L, catalogos.buscar(ListaPlantilla.CIUDAD, "Medellín").id());
        assertEquals(21L, catalogos.buscar(ListaPlantilla.CIUDAD, "San Luis – Tolima").id());
    }

    @Test
    void ciudadRepetidaSinDepartamento_pideElDepartamento() {
        CatalogosCarga.Busqueda busqueda = catalogos.buscar(ListaPlantilla.CIUDAD, "San Luis");
        assertNull(busqueda.id());
        assertTrue(busqueda.error().contains("varios departamentos"));
    }

    @Test
    void valorQueNoEstaEnLaLista() {
        CatalogosCarga.Busqueda busqueda = catalogos.buscar(ListaPlantilla.NIVEL_ACADEMICO, "Bachiller");
        assertNull(busqueda.id());
        assertEquals("'Bachiller' no está en la lista", busqueda.error());
    }

}
