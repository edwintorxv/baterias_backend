package com.riesgopsicosocial.domain.service.resultado;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
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

    private static final FiltrosAplicacion SIN_FILTROS = FiltrosAplicacion.SIN_REGISTRO;

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
                respuesta(1L, 10L, "1"), respuesta(2L, 10L, "2"), respuesta(3L, 11L, "4")), SIN_FILTROS);

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
                respuesta(1L, 20L, "9"), respuesta(2L, 20L, "3"), respuesta(3L, 21L, "0")), SIN_FILTROS);

        assertEquals(bd("24.00"), r.puntajeBruto());
        assertEquals(bd("39.2"), r.puntajeTransformado());
        assertTrue(r.dimensiones().isEmpty());
        assertTrue(r.dominios().isEmpty());
    }

    @Test
    void dimensionIncompleta_lanzaExcepcion() {
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "8", null, 2));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "1")), SIN_FILTROS));
        assertTrue(ex.getMessage().contains("1 de 2"));
    }

    @Test
    void puntajeFueraDeBaremo_lanzaExcepcion() {
        // 5 / 4 × 100 = 125, fuera de todos los rangos
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "4", null, 1));

        assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "5")), SIN_FILTROS));
    }

    @Test
    void sinBaremoTotal_lanzaExcepcion() {
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA,
                bd("8"), 2L, new ArrayList<>(List.of(dimension(10L, null, "8", null, 1))), List.of(), List.of());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "1")), SIN_FILTROS));
        assertTrue(ex.getMessage().contains("grupo ocupacional 2"));
    }

    // ------------------------------------------------------------------
    // Ítems condicionales (pregunta filtro) y faltantes
    // ------------------------------------------------------------------

    @Test
    void dimensionQueNoAplica_valeCeroYSumaAlDominio() {
        // Dimensión 10: 2 preguntas, factor 8. Dimensión 11 (atiende clientes): 2 preguntas, factor 8.
        // No atiende clientes → dim 11 = 0; dominio = 3 + 0 = 3 / 16 × 100 = 18.8
        ConfiguracionCuestionario config = sumaPorDominios(
                dimension(10L, 100L, "8", null, 2),
                dimension(11L, 100L, "8", 2, CondicionAplicacion.ATIENDE_CLIENTES, 0));

        ResultadoCuestionario r = calculadora.calcular(config,
                List.of(respuesta(1L, 10L, "1"), respuesta(2L, 10L, "2")),
                new FiltrosAplicacion(false, null));

        ResultadoDimension noAplica = r.dimensiones().get(1);
        assertEquals(bd("0.00"), noAplica.puntajeBruto());
        assertEquals(bd("0.0"), noAplica.puntajeTransformado());
        assertEquals(BAJO, noAplica.nivelRiesgo());
        assertEquals(bd("3.00"), r.dominios().get(0).puntajeBruto());
        assertEquals(bd("18.8"), r.dominios().get(0).puntajeTransformado());
        assertEquals(bd("3.00"), r.puntajeBruto());
    }

    @Test
    void dimensionQueNoAplica_conRespuestas_lanzaExcepcion() {
        ConfiguracionCuestionario config = sumaDirecta(
                dimension(11L, null, "8", 2, CondicionAplicacion.ES_JEFE, 0));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 11L, "1")),
                        new FiltrosAplicacion(null, false)));
        assertTrue(ex.getMessage().contains("no aplica (ES_JEFE = no) pero tiene 1 respuestas"));
    }

    @Test
    void dimensionConCondicion_filtroNoRegistradoOSi_exigeRespuestas() {
        ConfiguracionCuestionario config = sumaDirecta(
                dimension(11L, null, "8", 2, CondicionAplicacion.ATIENDE_CLIENTES, 0));

        for (FiltrosAplicacion filtros : List.of(SIN_FILTROS, new FiltrosAplicacion(true, null))) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> calculadora.calcular(config, List.of(), filtros));
            assertTrue(ex.getMessage().contains("0 de 2"), "filtros " + filtros);
        }
    }

    @Test
    void itemFaltanteDentroDeTolerancia_sumaLoRespondidoConElMismoFactor() {
        // 3 preguntas, factor 12, se admite 1 sin respuesta: (4 + 2) / 12 × 100 = 50.0
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "12", 3, null, 1));

        ResultadoCuestionario r = calculadora.calcular(config,
                List.of(respuesta(1L, 10L, "4"), respuesta(2L, 10L, "2")), SIN_FILTROS);

        assertEquals(bd("6.00"), r.dimensiones().get(0).puntajeBruto());
        assertEquals(bd("50.0"), r.dimensiones().get(0).puntajeTransformado());
    }

    @Test
    void itemsFaltantesSobreTolerancia_lanzaExcepcion() {
        ConfiguracionCuestionario config = sumaDirecta(dimension(10L, null, "12", 3, null, 1));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config, List.of(respuesta(1L, 10L, "4")), SIN_FILTROS));
        assertTrue(ex.getMessage().contains("1 de 3 preguntas respondidas (se admite hasta 1 sin respuesta)"));
    }

    @Test
    void promedioPonderado_conCondicionOTolerancia_lanzaExcepcion() {
        ConfiguracionCuestionario config = new ConfiguracionCuestionario(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO,
                bd("61.16"), 1L,
                List.of(dimension(20L, null, "1", 1, CondicionAplicacion.ATIENDE_CLIENTES, 0),
                        dimension(21L, null, "1", 1, null, 1)),
                List.of(), BAREMO);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular(config,
                        List.of(respuesta(1L, 20L, "9"), respuesta(2L, 21L, "0")), SIN_FILTROS));
        assertTrue(ex.getMessage().contains("'Dimensión 20', 'Dimensión 21'"));
    }

    @Test
    void filtrosAplicacion_soloNoAplicaConFalseExplicito() {
        assertTrue(new FiltrosAplicacion(false, null).noAplica(CondicionAplicacion.ATIENDE_CLIENTES));
        assertFalse(new FiltrosAplicacion(false, null).noAplica(CondicionAplicacion.ES_JEFE));
        assertFalse(new FiltrosAplicacion(true, true).noAplica(CondicionAplicacion.ES_JEFE));
        assertFalse(SIN_FILTROS.noAplica(CondicionAplicacion.ATIENDE_CLIENTES));
    }

    // ------------------------------------------------------------------
    // Total general (intralaboral + extralaboral)
    // ------------------------------------------------------------------

    private static final NivelRiesgo SIN_RIESGO = new NivelRiesgo(1L, "Sin riesgo o riesgo despreciable");
    private static final NivelRiesgo MEDIO = new NivelRiesgo(3L, "Riesgo medio");
    private static final NivelRiesgo MUY_ALTO = new NivelRiesgo(5L, "Riesgo muy alto");
    /** Tabla 34, fila A + C. */
    private static final List<RangoBaremo> BAREMO_TOTAL_A = List.of(
            new RangoBaremo(bd("0.0"), bd("18.8"), SIN_RIESGO),
            new RangoBaremo(bd("18.9"), bd("24.4"), BAJO),
            new RangoBaremo(bd("24.5"), bd("29.5"), MEDIO),
            new RangoBaremo(bd("29.6"), bd("35.4"), ALTO),
            new RangoBaremo(bd("35.5"), bd("100"), MUY_ALTO));

    @Test
    void totalGeneral_ejemplo4DelManual() {
        // Manual extralaboral, ejemplo 4: bruto general 306 con forma A → 306 / 616 × 100 = 49,7 (muy alto)
        ResultadoTotalGeneral r = calculadora.calcularTotalGeneral(
                total(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS, "492"), resultado(1L, "A", "250"),
                total(3L, "C", MetodoCalculo.SUMA_DIRECTA, "124"), resultado(3L, "C", "56"),
                BAREMO_TOTAL_A);

        assertEquals(bd("306.00"), r.puntajeBruto());
        assertEquals(bd("49.7"), r.puntajeTransformado());
        assertEquals(MUY_ALTO, r.nivelRiesgo());
        assertEquals(1L, r.idCuestionarioIntralaboral());
        assertEquals("A", r.formaIntralaboral());
    }

    @Test
    void totalGeneral_formaB_usaLaSumaDeFactores() {
        // 388 + 124 = 512: (100 + 28) / 512 × 100 = 25,0
        ResultadoTotalGeneral r = calculadora.calcularTotalGeneral(
                total(2L, "B", MetodoCalculo.SUMA_POR_DOMINIOS, "388"), resultado(2L, "B", "100"),
                total(3L, "C", MetodoCalculo.SUMA_DIRECTA, "124"), resultado(3L, "C", "28"),
                BAREMO_TOTAL_A);

        assertEquals(bd("25.0"), r.puntajeTransformado());
        assertEquals("B", r.formaIntralaboral());
    }

    @Test
    void totalGeneral_sinBaremo_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> calculadora.calcularTotalGeneral(
                total(2L, "B", MetodoCalculo.SUMA_POR_DOMINIOS, "388"), resultado(2L, "B", "100"),
                total(3L, "C", MetodoCalculo.SUMA_DIRECTA, "124"), resultado(3L, "C", "28"),
                List.of()));
        assertTrue(ex.getMessage().contains("No hay baremo para total general (B + C) y forma intralaboral B"));
    }

    @Test
    void totalGeneral_conCuestionariosQueNoSonIntraYExtra_lanzaExcepcion() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> calculadora.calcularTotalGeneral(
                total(3L, "C", MetodoCalculo.SUMA_DIRECTA, "124"), resultado(3L, "C", "28"),
                total(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO, "61.16"), resultado(4L, "D", "10"),
                BAREMO_TOTAL_A));
        assertTrue(ex.getMessage().contains("se recibieron C y D"));
    }

    /** Configuración mínima: el total general solo usa id, forma, método y factor. */
    private static ConfiguracionCuestionario total(Long id, String forma, MetodoCalculo metodo, String factor) {
        return new ConfiguracionCuestionario(id, forma, metodo, bd(factor), 1L, List.of(), List.of(), List.of());
    }

    private static ResultadoCuestionario resultado(Long id, String forma, String bruto) {
        return new ResultadoCuestionario(id, forma, bd(bruto), bd("0"), BAJO, List.of(), List.of());
    }

    private static ConfiguracionCuestionario sumaPorDominios(ConfiguracionDimension... dimensiones) {
        return new ConfiguracionCuestionario(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS, bd("16"), 1L,
                List.of(dimensiones), List.of(new ConfiguracionDominio(100L, "Dominio", bd("16"), BAREMO)), BAREMO);
    }

    private static ConfiguracionCuestionario sumaDirecta(ConfiguracionDimension dimension) {
        return new ConfiguracionCuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA, bd("100"), 1L,
                List.of(dimension), List.of(), BAREMO);
    }

    private static ConfiguracionDimension dimension(Long id, Long idDominio, String factor, String peso, int preguntas) {
        return new ConfiguracionDimension(id, idDominio, "Dimensión " + id, bd(factor),
                peso == null ? null : bd(peso), preguntas, null, 0, BAREMO);
    }

    private static ConfiguracionDimension dimension(Long id, Long idDominio, String factor, int preguntas,
                                                    CondicionAplicacion condicion, int maxItemsSinRespuesta) {
        return new ConfiguracionDimension(id, idDominio, "Dimensión " + id, bd(factor), null, preguntas,
                condicion, maxItemsSinRespuesta, BAREMO);
    }

    private static RespuestaCalculo respuesta(Long idPregunta, Long idDimension, String valor) {
        return new RespuestaCalculo(idPregunta, idDimension, 1L, bd(valor));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

}
