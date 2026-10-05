package com.riesgopsicosocial.domain.service.resultado;

import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraResultadoTest {

    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo ALTO = new NivelRiesgo(4L, "Riesgo alto");
    /** 0–50 bajo, 50.1–100 alto. */
    private static final List<RangoBaremo> BAREMO = List.of(
            new RangoBaremo(bd("0"), bd("50.0"), BAJO),
            new RangoBaremo(bd("50.1"), bd("100"), ALTO));

    private final CalculadoraResultado calculadora = new CalculadoraResultado();

    @Test
    void sumaPorDominios_calculaDimensionDominioYTotal() {
        // Dominio 100 con dimensiones 10 (2 preguntas, factor 8) y 11 (1 pregunta, factor 4)
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS,
                bd("12"), 1L,
                List.of(dimension(10L, 100L, "8", null, 2), dimension(11L, 100L, "4", null, 1)),
                List.of(new ConfiguracionDominio(100L, "Dominio", bd("12"), BAREMO)),
                BAREMO);

        ResultadoCuestionario r = calculadora.calcular(config, List.of(
                respuesta(1L, 10L, "1"), respuesta(2L, 10L, "2"), respuesta(3L, 11L, "4")));

        assertEquals(bd("3.00"), r.dimensiones().get(0).puntajeBruto());
        assertEquals(bd("37.5"), r.dimensiones().get(0).puntajeTransformado());
        assertEquals(BAJO, r.dimensiones().get(0).nivelRiesgo());
        assertEquals(bd("100.0"), r.dimensiones().get(1).puntajeTransformado());
        assertEquals(ALTO, r.dimensiones().get(1).nivelRiesgo());

        assertEquals(bd("7.00"), r.dominios().get(0).puntajeBruto());
        assertEquals(bd("58.3"), r.dominios().get(0).puntajeTransformado());
        assertEquals(bd("58.3"), r.puntajeTransformado());
        assertEquals(ALTO, r.nivelRiesgo());
    }

    @Test
    void promedioPonderado_sumaPromediosPorPesoYNoDevuelveDimensiones() {
        // Dim 1: promedio (9+3)/2 = 6 × 4 = 24 ; Dim 2: promedio 0 × 3 = 0 → bruto 24, 24/61.16×100 = 39.2
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO,
                bd("61.16"), 1L,
                List.of(dimension(20L, null, "1", "4", 2), dimension(21L, null, "1", "3", 1)),
                List.of(), BAREMO);

        ResultadoCuestionario r = calculadora.calcular(config, List.of(
                respuesta(1L, 20L, "9"), respuesta(2L, 20L, "3"), respuesta(3L, 21L, "0")));

        assertEquals(bd("24.00"), r.puntajeBruto());
        assertEquals(bd("39.2"), r.puntajeTransformado());
        assertTrue(r.dimensiones().isEmpty());
        assertTrue(r.dominios().isEmpty());
    }

    @Test
    void dimensionIncompleta_lanzaExcepcion() {
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "8", null, 2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "1"))));
        assertTrue(ex.getMessage().contains("1 de 2"));
    }

    @Test
    void puntajeFueraDeBaremo_lanzaExcepcion() {
        // 5 / 4 × 100 = 125, fuera de todos los rangos
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "4", null, 1));

        assertThrows(BusinessException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "5"))));
    }

    @Test
    void sinBaremoTotal_lanzaExcepcion() {
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA,
                bd("8"), 2L, new ArrayList<>(List.of(dimension(10L, null, "8", null, 1))), List.of(), List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "1"))));
        assertTrue(ex.getMessage().contains("grupo ocupacional 2"));
    }

    private static ConfiguracionCuestionario sumaDirecta(ConfiguracionDimension dimension) {
        return new ConfiguracionCuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA, bd("100"), 1L,
                List.of(dimension), List.of(), BAREMO);
    }

    private static ConfiguracionDimension dimension(Long id, Long idDominio, String factor, String peso, int preguntas) {
        return new ConfiguracionDimension(id, idDominio, "Dimensión " + id, bd(factor),
                peso == null ? null : bd(peso), preguntas, BAREMO);
    }

    private static RespuestaCalculo respuesta(Long idPregunta, Long idDimension, String valor) {
        return new RespuestaCalculo(idPregunta, idDimension, 1L, bd(valor));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

}
