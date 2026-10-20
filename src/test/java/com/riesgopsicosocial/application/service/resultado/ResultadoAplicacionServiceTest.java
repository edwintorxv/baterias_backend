package com.riesgopsicosocial.application.service.resultado;

import com.riesgopsicosocial.application.port.out.resultado.DatosCalculoPort;
import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Solo se calcula la aplicación completa, con todos los cuestionarios de su grupo. */
class ResultadoAplicacionServiceTest {

    private static final long APLICACION = 7L;
    private static final long GRUPO = 2L;

    private final DatosCalculoPort datos = mock(DatosCalculoPort.class);
    private final ResultadoPort resultados = mock(ResultadoPort.class);
    private final ResultadoAplicacionService service = new ResultadoAplicacionService(datos, resultados);

    @BeforeEach
    void preparar() {
        when(datos.buscarGrupoOcupacional(APLICACION)).thenReturn(Optional.of(GRUPO));
        when(datos.buscarFiltros(APLICACION)).thenReturn(FiltrosAplicacion.SIN_REGISTRO);
        when(datos.buscarCuestionariosDelGrupo(GRUPO)).thenReturn(List.of(3L, 4L));
        when(datos.cargarConfiguracion(3L, GRUPO)).thenReturn(cuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA, 30L));
        when(datos.cargarConfiguracion(4L, GRUPO)).thenReturn(cuestionario(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO, 40L));
    }

    @Test
    void sinUnCuestionarioDelGrupoNoCalculaNada() {
        // C completo, D sin ninguna respuesta: antes se calculaba solo C
        when(datos.buscarRespuestas(APLICACION)).thenReturn(List.of(
                new RespuestaCalculo(1L, 30L, 3L, BigDecimal.ONE), new RespuestaCalculo(2L, 30L, 3L, BigDecimal.ONE)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.calcular(APLICACION));

        assertTrue(ex.getMessage().contains("faltan 2 preguntas del cuestionario D"), ex.getMessage());
        verify(resultados, never()).reemplazar(any());
    }

    @Test
    void respuestasDeOtroGrupoNoSeCalculan() {
        when(datos.buscarRespuestas(APLICACION)).thenReturn(List.of(new RespuestaCalculo(1L, 10L, 1L, BigDecimal.ONE)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.calcular(APLICACION));

        assertTrue(ex.getMessage().contains("no son de su grupo ocupacional"), ex.getMessage());
        verify(resultados, never()).reemplazar(any());
    }

    private static ConfiguracionCuestionario cuestionario(Long id, String forma, MetodoCalculo metodo, Long idDimension) {
        ConfiguracionDimension dimension = new ConfiguracionDimension(idDimension, null, "Dimensión " + idDimension,
                BigDecimal.TEN, BigDecimal.ONE, 2, null, 0, List.of());
        return new ConfiguracionCuestionario(id, forma, metodo, BigDecimal.TEN, GRUPO, List.of(dimension), List.of(), List.of());
    }

}
