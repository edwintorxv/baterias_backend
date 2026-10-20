package com.riesgopsicosocial.domain.service.informe;

import com.riesgopsicosocial.domain.model.informe.GrupoSintomas;
import com.riesgopsicosocial.domain.model.informe.SintomaFrecuente;
import com.riesgopsicosocial.domain.model.informe.empresa.DimensionInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ItemSintoma;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeleccionSintomasFrecuentesTest {

    private static final List<DimensionInforme> DIMENSIONES = List.of(
            new DimensionInforme(50L, "Fisiológicos", null),
            new DimensionInforme(51L, "Comportamiento social", null),
            new DimensionInforme(52L, "Psicoemocionales", null));
    private static final List<ItemSintoma> ITEMS = List.of(
            new ItemSintoma(501L, 50L, 1, "Dolor de cabeza"),
            new ItemSintoma(502L, 50L, 2, "Problemas gastrointestinales"),
            new ItemSintoma(503L, 50L, 3, "Problemas de sueño"),
            new ItemSintoma(511L, 51L, 11, "Aislarse"),
            new ItemSintoma(521L, 52L, 21, "Angustia"));
    private static final List<OpcionFrecuencia> OPCIONES = List.of(
            new OpcionFrecuencia(1L, "Siempre"), new OpcionFrecuencia(2L, "Casi siempre"),
            new OpcionFrecuencia(3L, "Algunas veces"), new OpcionFrecuencia(5L, "Nunca"));

    @Test
    void soloSiempreYCasiSiempre_siempreAntesYLuegoPorNumero() {
        List<GrupoSintomas> grupos = SeleccionSintomasFrecuentes.seleccionar(DIMENSIONES, ITEMS, List.of(
                respuesta(501L, 50L, 2L),
                respuesta(502L, 50L, 3L),
                respuesta(503L, 50L, 1L),
                respuesta(511L, 51L, 5L),
                respuesta(521L, 52L, 2L)), OPCIONES);

        assertEquals(List.of("Fisiológicos", "Comportamiento social", "Psicoemocionales"),
                grupos.stream().map(GrupoSintomas::nombre).toList());
        assertEquals(List.of(new SintomaFrecuente(3, "Problemas de sueño", "Siempre"),
                new SintomaFrecuente(1, "Dolor de cabeza", "Casi siempre")), grupos.get(0).sintomas());
        assertTrue(grupos.get(1).sintomas().isEmpty(), "sin síntomas frecuentes: el grupo queda vacío");
        assertEquals(List.of(new SintomaFrecuente(21, "Angustia", "Casi siempre")), grupos.get(2).sintomas());
    }

    @Test
    void empateEnFrecuencia_ordenDelCuestionario() {
        List<GrupoSintomas> grupos = SeleccionSintomasFrecuentes.seleccionar(DIMENSIONES, ITEMS, List.of(
                respuesta(503L, 50L, 1L), respuesta(501L, 50L, 1L)), OPCIONES);

        assertEquals(List.of(1, 3), grupos.get(0).sintomas().stream().map(SintomaFrecuente::numero).toList());
    }

    @Test
    void sinRespuestas_listaVacia() {
        assertTrue(SeleccionSintomasFrecuentes.seleccionar(DIMENSIONES, ITEMS, List.of(), OPCIONES).isEmpty());
    }

    private static RespuestaSintoma respuesta(Long idPregunta, Long idDimension, Long idOpcion) {
        return new RespuestaSintoma(1L, idPregunta, idDimension, idOpcion);
    }

}
