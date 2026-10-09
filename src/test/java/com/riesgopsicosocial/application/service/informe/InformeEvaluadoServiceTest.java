package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InformeEvaluadoServiceTest {

    private static final DatosCliente CLIENTE = new DatosCliente(1L, "900", "Empresa X");
    private static final DatosEvaluado EVALUADO = new DatosEvaluado(7L, "123", "Ana", "Pérez", "Femenino", 1990);

    private final PuertoFalso puerto = new PuertoFalso();
    private final ResultadosFalsos resultados = new ResultadosFalsos();
    private final InformeEvaluadoService service = new InformeEvaluadoService(puerto, resultados);

    @Test
    void ordenaPorFechaYMarcaPendientes() {
        puerto.aplicaciones.add(aplicacion(3L, "2026-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(1L, "2024-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(2L, "2024-03-01T08:00"));
        resultados.conResultados.addAll(List.of(1L, 3L));

        InformeEvaluado informe = service.consultar(1L, 7L, null);

        assertEquals(CLIENTE, informe.cliente());
        assertEquals(EVALUADO, informe.evaluado());
        assertEquals(List.of(1L, 2L, 3L), informe.aplicaciones().stream().map(a -> a.datos().idAplicacion()).toList());
        assertEquals(List.of(true, false, true), informe.aplicaciones().stream().map(AplicacionEvaluado::tieneResultados).toList());
    }

    @Test
    void edadSeCalculaConElAnioDeCadaAplicacion() {
        puerto.aplicaciones.add(aplicacion(1L, "2024-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(2L, "2026-03-01T08:00"));

        InformeEvaluado informe = service.consultar(1L, 7L, null);

        assertEquals(List.of(34, 36), informe.aplicaciones().stream().map(AplicacionEvaluado::edad).toList());
    }

    @Test
    void edadSinAnioDeNacimiento_esNula() {
        assertNull(new DatosEvaluado(7L, "123", "Ana", "Pérez", null, null).edadEn(2026));
    }

    @Test
    void sinAplicaciones_devuelveListaVacia() {
        assertTrue(service.consultar(1L, 7L, null).aplicaciones().isEmpty());
    }

    @Test
    void clienteInexistente_lanza404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.consultar(99L, 7L, null));
        assertTrue(ex.getMessage().contains("Cliente"));
    }

    @Test
    void evaluadoInexistente_lanza404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.consultar(1L, 99L, null));
        assertTrue(ex.getMessage().contains("Evaluado"));
    }

    @Test
    void sinRelacionConElCliente_lanza404() {
        puerto.hayRelacion = false;
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.consultar(1L, 7L, null));
        assertTrue(ex.getMessage().contains("no tiene relación"));
    }

    @Test
    void conAnio_soloIncluyeLasAplicacionesDeEseAnio() {
        puerto.aplicaciones.add(aplicacion(1L, "2024-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(2L, "2026-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(3L, "2026-01-15T08:00"));

        InformeEvaluado informe = service.consultar(1L, 7L, 2026);

        assertEquals(List.of(3L, 2L), informe.aplicaciones().stream().map(a -> a.datos().idAplicacion()).toList());
    }

    @Test
    void anioSinAplicaciones_lanza404() {
        puerto.aplicaciones.add(aplicacion(1L, "2024-03-01T08:00"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.consultar(1L, 7L, 2025));
        assertTrue(ex.getMessage().contains("en el año 2025"));
    }

    @Test
    void anios_distintosDelMasRecienteAlMasAntiguo() {
        puerto.aplicaciones.add(aplicacion(1L, "2022-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(2L, "2026-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(3L, "2024-03-01T08:00"));
        puerto.aplicaciones.add(aplicacion(4L, "2026-09-01T08:00"));

        assertEquals(List.of(2026, 2024, 2022), service.consultarAnios(1L, 7L));
    }

    @Test
    void anios_sinRelacionConElCliente_lanza404() {
        puerto.hayRelacion = false;
        assertThrows(ResourceNotFoundException.class, () -> service.consultarAnios(1L, 7L));
    }

    private static DatosAplicacion aplicacion(Long id, String fecha) {
        return new DatosAplicacion(id, LocalDateTime.parse(fecha), "Analista", "Talento humano", 1L, "Grupo 1",
                null, null, null);
    }

    private static class PuertoFalso implements InformeEvaluadoPort {
        final List<DatosAplicacion> aplicaciones = new ArrayList<>();
        boolean hayRelacion = true;

        @Override
        public Optional<DatosCliente> buscarCliente(Long idCliente) {
            return idCliente.equals(CLIENTE.id()) ? Optional.of(CLIENTE) : Optional.empty();
        }

        @Override
        public Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado) {
            return idEvaluado.equals(EVALUADO.id()) ? Optional.of(EVALUADO) : Optional.empty();
        }

        @Override
        public boolean existeRelacion(Long idCliente, Long idEvaluado) {
            return hayRelacion;
        }

        @Override
        public List<DatosAplicacion> buscarAplicaciones(Long idCliente, Long idEvaluado) {
            return aplicaciones;
        }
    }

    private static class ResultadosFalsos implements ResultadoPort {
        final Set<Long> conResultados = new HashSet<>();

        @Override
        public void reemplazar(ResultadoAplicacion resultado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultadoAplicacion buscarPorAplicacion(Long idAplicacion, Long idGrupoOcupacional) {
            List<ResultadoCuestionario> cuestionarios = conResultados.contains(idAplicacion)
                    ? List.of(new ResultadoCuestionario(1L, "A", BigDecimal.ONE, BigDecimal.ONE,
                    new NivelRiesgo(1L, "Sin riesgo"), List.of(), List.of()))
                    : List.of();
            return new ResultadoAplicacion(idAplicacion, idGrupoOcupacional, null, cuestionarios, null);
        }
    }

}
