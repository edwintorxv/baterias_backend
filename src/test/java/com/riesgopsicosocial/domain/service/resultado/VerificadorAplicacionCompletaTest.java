package com.riesgopsicosocial.domain.service.resultado;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class VerificadorAplicacionCompletaTest {

    private static final FiltrosAplicacion SIN_FILTROS = FiltrosAplicacion.SIN_REGISTRO;

    /** A: dimensión 10 (3 preguntas, admite 1 sin respuesta en el manual) y 11 condicionada a atender clientes (2). */
    private static final ConfiguracionCuestionario A = cuestionario(1L, "A",
            dimension(10L, 3, null, 1), dimension(11L, 2, CondicionAplicacion.ATIENDE_CLIENTES, 0));
    private static final ConfiguracionCuestionario C = cuestionario(3L, "C", dimension(30L, 2, null, 0));

    @Test
    void completaNoLanza() {
        List<RespuestaCalculo> respuestas = concatenar(respuestas(1L, 10L, 1, 3), respuestas(1L, 11L, 4, 5), respuestas(3L, 30L, 6, 7));
        assertDoesNotThrow(() -> VerificadorAplicacionCompleta.exigirCompleta(List.of(A, C), respuestas, SIN_FILTROS));
    }

    @Test
    void laToleranciaDelManualNoAlcanza() {
        // 2 de 3 en la dimensión 10: el motor lo admitiría, pero no se calcula incompleta
        List<RespuestaCalculo> respuestas = concatenar(respuestas(1L, 10L, 1, 2), respuestas(1L, 11L, 4, 5), respuestas(3L, 30L, 6, 7));
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> VerificadorAplicacionCompleta.exigirCompleta(List.of(A, C), respuestas, SIN_FILTROS));
        assertTrue(ex.getMessage().contains("falta 1 pregunta del cuestionario A."), ex.getMessage());
    }

    @Test
    void cuentaLasFaltantesDeCadaCuestionarioIncluidoElSinRespuestas() {
        List<RespuestaCalculo> respuestas = respuestas(1L, 10L, 1, 3);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> VerificadorAplicacionCompleta.exigirCompleta(List.of(A, C), respuestas, SIN_FILTROS));
        assertTrue(ex.getMessage().contains("faltan 2 preguntas del cuestionario A y 2 preguntas del cuestionario C"), ex.getMessage());
    }

    @Test
    void laDimensionConPreguntaFiltroEnNoNoSeExige() {
        List<RespuestaCalculo> respuestas = concatenar(respuestas(1L, 10L, 1, 3), respuestas(3L, 30L, 6, 7));
        assertDoesNotThrow(() -> VerificadorAplicacionCompleta.exigirCompleta(List.of(A, C), respuestas,
                new FiltrosAplicacion(false, null)));
        assertThrows(ReglaNegocioException.class,
                () -> VerificadorAplicacionCompleta.exigirCompleta(List.of(A, C), respuestas, SIN_FILTROS));
    }

    private static ConfiguracionCuestionario cuestionario(Long id, String forma, ConfiguracionDimension... dimensiones) {
        return new ConfiguracionCuestionario(id, forma, MetodoCalculo.SUMA_DIRECTA, BigDecimal.TEN, 1L,
                List.of(dimensiones), List.of(), List.of());
    }

    private static ConfiguracionDimension dimension(Long id, int preguntas, CondicionAplicacion condicion, int maxSinRespuesta) {
        return new ConfiguracionDimension(id, null, "Dimensión " + id, BigDecimal.TEN, null, preguntas, condicion,
                maxSinRespuesta, List.of());
    }

    /** Preguntas [desde, hasta] respondidas en esa dimensión. */
    private static List<RespuestaCalculo> respuestas(Long idCuestionario, Long idDimension, long desde, long hasta) {
        return LongStream.rangeClosed(desde, hasta)
                .mapToObj(p -> new RespuestaCalculo(p, idDimension, idCuestionario, BigDecimal.ONE))
                .toList();
    }

    @SafeVarargs
    private static List<RespuestaCalculo> concatenar(List<RespuestaCalculo>... listas) {
        return java.util.Arrays.stream(listas).flatMap(List::stream).toList();
    }

}
