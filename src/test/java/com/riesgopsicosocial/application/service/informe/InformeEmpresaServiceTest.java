package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.out.informe.InformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InformeEmpresaServiceTest {

    private static final DatosCliente CLIENTE = new DatosCliente(1L, "900", "Empresa X");
    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo ALTO = new NivelRiesgo(4L, "Riesgo alto");

    private final PuertoFalso puerto = new PuertoFalso();
    private final InformeEmpresaService service = new InformeEmpresaService(puerto);

    @Test
    void esperadosSonLasPersonasCargadasAunqueNoTenganAplicacion() {
        puerto.personas.addAll(List.of(new PersonaCargada(10L, "A", null, FichaPersona.SIN_DATOS), new PersonaCargada(11L, "A", null, FichaPersona.SIN_DATOS),
                new PersonaCargada(12L, "A", null, FichaPersona.SIN_DATOS)));
        puerto.aplicaciones.add(aplicacion(1L, 10L, "2026-03-01T08:00", true, intralaboral(ALTO)));
        puerto.aplicaciones.add(aplicacion(2L, 11L, "2026-03-01T08:00", true, null));

        InformeEmpresa informe = service.consultar(1L, 2026);

        assertEquals(CLIENTE, informe.cliente());
        assertEquals(2026, informe.anio());
        assertEquals(new Cobertura(3, 2, new BigDecimal("66.7")), informe.general().cobertura());
        assertEquals(1, informe.general().pendientesCalculo());
        assertEquals(1, informe.general().intralaboral().total());
    }

    @Test
    void variasAplicacionesEnElAnio_cuentaUnaPersonaConSuResultadoMasReciente() {
        puerto.personas.add(new PersonaCargada(10L, "A", null, FichaPersona.SIN_DATOS));
        puerto.aplicaciones.add(aplicacion(1L, 10L, "2026-03-01T08:00", true, intralaboral(BAJO)));
        puerto.aplicaciones.add(aplicacion(2L, 10L, "2026-09-01T08:00", true, intralaboral(ALTO)));
        puerto.aplicaciones.add(aplicacion(3L, 10L, "2026-11-01T08:00", true, null)); // sin calcular: no reemplaza

        AnalisisGrupo general = service.consultar(1L, 2026).general();

        assertEquals(new Cobertura(1, 1, new BigDecimal("100.0")), general.cobertura());
        assertEquals(0, general.pendientesCalculo());
        assertEquals(1, general.intralaboral().total());
        assertEquals(1, general.intralaboral().niveles().get(1).cantidad(), "debe contar el nivel alto");
    }

    @Test
    void aplicacionesDeRelacionesDeOtroCliente_noCuentan() {
        puerto.personas.add(new PersonaCargada(10L, "A", null, FichaPersona.SIN_DATOS));
        puerto.aplicaciones.add(aplicacion(1L, 99L, "2026-03-01T08:00", true, intralaboral(ALTO)));

        AnalisisGrupo general = service.consultar(1L, 2026).general();

        assertEquals(new Cobertura(1, 0, new BigDecimal("0.0")), general.cobertura());
        assertEquals(0, general.intralaboral().total());
    }

    @Test
    void retiradoQueSeEvaluoEnElAnio_sigueContando() {
        puerto.personas.add(persona(10L, "2024-01-15", "2025-08-01")); // se evaluó y luego se fue
        puerto.personas.add(persona(11L, "2024-01-15", null));
        puerto.aplicaciones.add(aplicacion(1L, 10L, "2025-03-01T08:00", true, intralaboral(ALTO)));
        puerto.aplicaciones.add(aplicacion(2L, 11L, "2025-03-01T08:00", true, intralaboral(BAJO)));

        AnalisisGrupo general = service.consultar(1L, 2025).general();

        assertEquals(new Cobertura(2, 2, new BigDecimal("100.0")), general.cobertura());
        assertEquals(2, general.intralaboral().total());
    }

    @Test
    void esperados_soloLosVigentesAlCierreDelAnio() {
        puerto.personas.add(persona(10L, "2024-01-15", null));         // evaluada
        puerto.personas.add(persona(11L, "2024-01-15", null));         // vigente sin evaluar: esperada
        puerto.personas.add(persona(12L, "2024-01-15", "2025-04-30")); // se fue sin evaluarse: no se esperaba
        puerto.personas.add(persona(13L, "2026-02-01", null));         // llegó después: no se esperaba
        puerto.aplicaciones.add(aplicacion(1L, 10L, "2025-03-01T08:00", true, intralaboral(BAJO)));

        AnalisisGrupo general = service.consultar(1L, 2025).general();

        assertEquals(new Cobertura(2, 1, new BigDecimal("50.0")), general.cobertura());
    }

    @Test
    void clienteInexistente_404() {
        puerto.cliente = null;
        assertThrows(ResourceNotFoundException.class, () -> service.consultar(1L, 2026));
        assertThrows(ResourceNotFoundException.class, () -> service.consultarAnios(1L));
    }

    @Test
    void sinPersonasCargadas_409() {
        puerto.aplicaciones.add(aplicacion(1L, 10L, "2026-03-01T08:00", true, null));
        assertThrows(BusinessException.class, () -> service.consultar(1L, 2026));
    }

    @Test
    void anioSinAplicaciones_404() {
        puerto.personas.add(new PersonaCargada(10L, "A", null, FichaPersona.SIN_DATOS));
        assertThrows(ResourceNotFoundException.class, () -> service.consultar(1L, 2026));
    }

    @Test
    void anios_sinRepetirYDelMasReciente() {
        puerto.anios.addAll(List.of(2024, 2026, 2024, 2025));
        assertEquals(List.of(2026, 2025, 2024), service.consultarAnios(1L));
    }

    private static PersonaCargada persona(Long id, String vinculacion, String retiro) {
        return new PersonaCargada(id, "A", null, FichaPersona.SIN_DATOS, LocalDate.parse(vinculacion),
                retiro == null ? null : LocalDate.parse(retiro));
    }

    private static AplicacionEmpresa aplicacion(Long id, Long idEvaluadoCliente, String fecha, boolean tieneRespuestas,
                                                ResultadoAplicacion resultado) {
        return new AplicacionEmpresa(id, idEvaluadoCliente, LocalDateTime.parse(fecha), tieneRespuestas, resultado,
                FiltrosAplicacion.SIN_REGISTRO);
    }

    private static ResultadoAplicacion intralaboral(NivelRiesgo nivel) {
        return new ResultadoAplicacion(1L, 1L, null, List.of(new ResultadoCuestionario(1L, "A", BigDecimal.ZERO,
                BigDecimal.ZERO, nivel, List.of(), List.of())), null);
    }

    private static class PuertoFalso implements InformeEmpresaPort {

        DatosCliente cliente = CLIENTE;
        final List<PersonaCargada> personas = new ArrayList<>();
        final List<AplicacionEmpresa> aplicaciones = new ArrayList<>();
        final List<Integer> anios = new ArrayList<>();

        @Override
        public Optional<DatosCliente> buscarCliente(Long idCliente) {
            return Optional.ofNullable(cliente);
        }

        @Override
        public List<PersonaCargada> buscarPersonasCargadas(Long idCliente) {
            return personas;
        }

        @Override
        public List<AplicacionEmpresa> buscarAplicaciones(Long idCliente, int anio) {
            return aplicaciones;
        }

        @Override
        public List<Integer> buscarAnios(Long idCliente) {
            return anios;
        }

        @Override
        public List<CuestionarioInforme> buscarCuestionarios() {
            return List.of(new CuestionarioInforme(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS, List.of()));
        }

        @Override
        public List<ConfiguracionNivelEmpresa> buscarConfiguracionNivelEmpresa() {
            return List.of();
        }

        @Override
        public List<NivelRiesgo> buscarNiveles() {
            return List.of(BAJO, ALTO);
        }

        @Override
        public List<RespuestaSintoma> buscarRespuestasEstres(Collection<Long> idsAplicacion) {
            return List.of();
        }

        @Override
        public List<OpcionFrecuencia> buscarOpcionesEstres() {
            return List.of();
        }

        @Override
        public List<ItemSintoma> buscarItemsEstres() {
            return List.of();
        }

        @Override
        public Optional<ConclusionesInforme> buscarConclusiones(Long idCliente, int anio) {
            return Optional.empty();
        }

        @Override
        public Optional<FirmaEvaluador> buscarFirma(Long idEvaluador) {
            return Optional.empty();
        }

    }

}
