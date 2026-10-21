package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremototalgeneral.BaremoTotalGeneralEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremototalgeneral.BaremoTotalGeneralJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionJpaRespository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Cuestionario A de prueba (id 1, grupo 1): dominios 1 y 2 (guardados al revés); el 1 con las dimensiones 11
 * y 10 (la 11 depende de "atiende clientes") y el 2 con la 20. Preguntas guardadas desordenadas, con la escala
 * directa (1) y la invertida (2). C (id 3) es el extralaboral.
 */
class EstructuraCuestionarioServiceTest {

    private final CuestionarioJpaRepository cuestionarioRepository = mock(CuestionarioJpaRepository.class);
    private final DominioCuestionarioJpaRepository dominioCuestionarioRepository = mock(DominioCuestionarioJpaRepository.class);
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository = mock(DimensionCuestionarioJpaRepository.class);
    private final DominioJpaRepository dominioRepository = mock(DominioJpaRepository.class);
    private final DimensionJpaRespository dimensionRepository = mock(DimensionJpaRespository.class);
    private final PreguntaJpaRepository preguntaRepository = mock(PreguntaJpaRepository.class);
    private final EscalaJpaRepository escalaRepository = mock(EscalaJpaRepository.class);
    private final EscalaDetalleJpaRepository escalaDetalleRepository = mock(EscalaDetalleJpaRepository.class);
    private final OpcionRespuestaJpaRepository opcionRepository = mock(OpcionRespuestaJpaRepository.class);
    private final NivelRiesgoJpaRepository nivelRepository = mock(NivelRiesgoJpaRepository.class);
    private final BaremoCuestionarioJpaRepository baremoCuestionarioRepository = mock(BaremoCuestionarioJpaRepository.class);
    private final BaremoDominioJpaRepository baremoDominioRepository = mock(BaremoDominioJpaRepository.class);
    private final BaremoDimensionJpaRepository baremoDimensionRepository = mock(BaremoDimensionJpaRepository.class);
    private final BaremoTotalGeneralJpaRepository baremoTotalGeneralRepository = mock(BaremoTotalGeneralJpaRepository.class);
    private final EstructuraCuestionarioService service = new EstructuraCuestionarioService(cuestionarioRepository,
            dominioCuestionarioRepository, dimensionCuestionarioRepository, dominioRepository, dimensionRepository,
            preguntaRepository, escalaRepository, escalaDetalleRepository, opcionRepository, nivelRepository,
            baremoCuestionarioRepository, baremoDominioRepository, baremoDimensionRepository, baremoTotalGeneralRepository);

    private final CuestionarioEntity formaA = cuestionario(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS, "492");
    private final CuestionarioEntity formaC = cuestionario(3L, "C", MetodoCalculo.SUMA_DIRECTA, "124");

    @BeforeEach
    void configuracion() {
        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(formaA));
        when(cuestionarioRepository.findAll()).thenReturn(List.of(formaC, formaA));
        when(nivelRepository.findAll()).thenReturn(List.of(nivel(1L, "Sin riesgo"), nivel(2L, "Riesgo bajo")));
        when(dominioRepository.findAll()).thenReturn(List.of(dominio(1L, "Liderazgo"), dominio(2L, "Control")));
        when(dimensionRepository.findAll()).thenReturn(List.of(
                dimension(10L, 1L, "Características del liderazgo"), dimension(11L, 1L, "Relación con los colaboradores"),
                dimension(20L, 2L, "Claridad de rol")));
        when(dominioCuestionarioRepository.findByFkCuestionario(1L)).thenReturn(List.of(
                dominioCuestionario(102L, 2L, "40"), dominioCuestionario(101L, 1L, "164")));
        when(dimensionCuestionarioRepository.findByFkCuestionario(1L)).thenReturn(List.of(
                dimensionCuestionario(111L, 11L, CondicionAplicacion.ATIENDE_CLIENTES),
                dimensionCuestionario(120L, 20L, null),
                dimensionCuestionario(110L, 10L, null)));
        when(preguntaRepository.findByFkDimensionCuestionarioIn(anyCollection())).thenReturn(List.of(
                pregunta(3L, 120L, 3, 2L), pregunta(2L, 110L, 2, 1L), pregunta(1L, 110L, 1, 2L), pregunta(4L, 111L, 4, 1L)));
        when(baremoCuestionarioRepository.findByFkCuestionario(1L)).thenReturn(List.of(
                baremoCuestionario(1L, 2L, "19.8", "25.8"), baremoCuestionario(1L, 1L, "0", "19.7")));
        when(baremoDominioRepository.findByFkDominioCuestionarioIn(anyCollection())).thenReturn(List.of(
                baremoDominio(101L, 1L, "0", "9.1")));
        when(baremoDimensionRepository.findByFkDimensionCuestionarioIn(anyCollection())).thenReturn(List.of(
                baremoDimension(110L, 2L, "3.9", "15.4"), baremoDimension(110L, 1L, "0", "3.8")));
        when(opcionRepository.findAll()).thenReturn(List.of(opcion(1L, "Siempre"), opcion(2L, "Nunca")));
        when(escalaDetalleRepository.findAll()).thenReturn(List.of(
                escalaDetalle(1L, 2L, "4"), escalaDetalle(1L, 1L, "0"), escalaDetalle(2L, 1L, "4"), escalaDetalle(2L, 2L, "0"),
                escalaDetalle(9L, 1L, "9")));
        when(escalaRepository.findAllById(anyCollection())).thenReturn(List.of(escala(2L, "Escala invertida"), escala(1L, "Escala directa")));
    }

    @Test
    void estructura_dominiosDimensionesYPreguntasEnElOrdenDelManual() {
        EstructuraCuestionario estructura = service.estructura(1L);

        assertEquals("A", estructura.forma());
        assertEquals(MetodoCalculo.SUMA_POR_DOMINIOS, estructura.metodoCalculo());
        assertEquals(4, estructura.totalPreguntas());
        assertEquals(List.of("Liderazgo", "Control"), estructura.dominios().stream().map(EstructuraCuestionario.Dominio::nombre).toList());

        EstructuraCuestionario.Dominio liderazgo = estructura.dominios().get(0);
        assertEquals(101L, liderazgo.id());
        assertEquals(new BigDecimal("164"), liderazgo.factorTransformacion());
        assertEquals(List.of(110L, 111L), liderazgo.dimensiones().stream().map(EstructuraCuestionario.Dimension::id).toList());
        EstructuraCuestionario.Dimension caracteristicas = liderazgo.dimensiones().get(0);
        assertEquals("Características del liderazgo", caracteristicas.nombre());
        assertEquals(List.of(1, 2), caracteristicas.preguntas().stream().map(EstructuraCuestionario.Pregunta::numero).toList());
        assertEquals(2L, caracteristicas.preguntas().get(0).idEscala());
        assertEquals(CondicionAplicacion.ATIENDE_CLIENTES, liderazgo.dimensiones().get(1).condicionAplicacion());

        assertEquals(List.of(120L), estructura.dominios().get(1).dimensiones().stream().map(EstructuraCuestionario.Dimension::id).toList());
    }

    @Test
    void estructura_baremosPorNivelConSuNombre() {
        EstructuraCuestionario estructura = service.estructura(1L);

        assertEquals(List.of(1L, 2L), estructura.baremos().stream().map(EstructuraCuestionario.Baremo::idNivelRiesgo).toList());
        assertEquals("Sin riesgo", estructura.baremos().get(0).nivelRiesgo());
        assertEquals(new BigDecimal("19.7"), estructura.baremos().get(0).valorMaximo());

        EstructuraCuestionario.Dominio liderazgo = estructura.dominios().get(0);
        assertEquals(1, liderazgo.baremos().size());
        assertEquals(List.of(1L, 2L), liderazgo.dimensiones().get(0).baremos().stream()
                .map(EstructuraCuestionario.Baremo::idNivelRiesgo).toList());
        assertTrue(liderazgo.dimensiones().get(1).baremos().isEmpty(), "sin baremos: lista vacía, no null");
        assertTrue(estructura.dominios().get(1).baremos().isEmpty());
    }

    @Test
    void estructura_soloLasEscalasUsadasConElValorDeCadaOpcion() {
        List<EstructuraCuestionario.Escala> escalas = service.estructura(1L).escalas();

        assertEquals(List.of(1L, 2L), escalas.stream().map(EstructuraCuestionario.Escala::id).toList());
        EstructuraCuestionario.Escala invertida = escalas.get(1);
        assertEquals("Escala invertida", invertida.nombre());
        assertEquals(List.of(new EstructuraCuestionario.Opcion(1L, "Siempre", new BigDecimal("4")),
                new EstructuraCuestionario.Opcion(2L, "Nunca", new BigDecimal("0"))), invertida.opciones());
        verify(escalaRepository).findAllById(argThat(ids -> ids instanceof java.util.Collection<?> c && c.size() == 2
                && c.containsAll(List.of(1L, 2L))));
    }

    @Test
    void estructura_cuestionarioInexistente_404() {
        when(cuestionarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.estructura(99L));
    }

    @Test
    void totalGeneral_unoPorFormaIntralaboralConElFactorSumadoYSuGrupo() {
        BaremoTotalGeneralEntity alto = new BaremoTotalGeneralEntity();
        alto.setFkCuestionarioIntralaboral(1L);
        alto.setFkNivelRiesgo(2L);
        alto.setValorMinimo(new BigDecimal("18.9"));
        alto.setValorMaximo(new BigDecimal("24.4"));
        BaremoTotalGeneralEntity sinRiesgo = new BaremoTotalGeneralEntity();
        sinRiesgo.setFkCuestionarioIntralaboral(1L);
        sinRiesgo.setFkNivelRiesgo(1L);
        sinRiesgo.setValorMinimo(BigDecimal.ZERO);
        sinRiesgo.setValorMaximo(new BigDecimal("18.8"));
        when(baremoTotalGeneralRepository.findAll()).thenReturn(List.of(alto, sinRiesgo));

        List<EstructuraCuestionario.TotalGeneral> total = service.totalGeneral();

        assertEquals(1, total.size(), "C no es intralaboral");
        EstructuraCuestionario.TotalGeneral a = total.get(0);
        assertEquals("A", a.formaIntralaboral());
        assertEquals("C", a.formaExtralaboral());
        assertEquals(new BigDecimal("616"), a.factorTransformacion());
        assertEquals(List.of(1L, 2L), a.baremos().stream().map(EstructuraCuestionario.Baremo::idNivelRiesgo).toList());
        assertEquals(1L, a.baremos().get(0).idGrupoOcupacional());
        assertEquals("Riesgo bajo", a.baremos().get(1).nivelRiesgo());
    }

    // ---- datos de prueba

    private static CuestionarioEntity cuestionario(Long id, String forma, MetodoCalculo metodo, String factor) {
        CuestionarioEntity c = new CuestionarioEntity();
        c.setId(id);
        c.setForma(forma);
        c.setMetodoCalculo(metodo);
        c.setFactorTransformacion(new BigDecimal(factor));
        return c;
    }

    private static NivelRiesgoEntity nivel(Long id, String nombre) {
        NivelRiesgoEntity n = new NivelRiesgoEntity();
        n.setId(id);
        n.setNombre(nombre);
        return n;
    }

    private static DominioEntity dominio(Long id, String nombre) {
        DominioEntity d = new DominioEntity();
        d.setId(id);
        d.setNombre(nombre);
        return d;
    }

    private static DimensionEntity dimension(Long id, Long dominio, String nombre) {
        DimensionEntity d = new DimensionEntity();
        d.setId(id);
        d.setFkDominio(dominio);
        d.setNombre(nombre);
        return d;
    }

    private static DominioCuestionarioEntity dominioCuestionario(Long id, Long dominio, String factor) {
        DominioCuestionarioEntity d = new DominioCuestionarioEntity();
        d.setId(id);
        d.setFkDominio(dominio);
        d.setFkCuestionario(1L);
        d.setFactorTransformacion(new BigDecimal(factor));
        return d;
    }

    private static DimensionCuestionarioEntity dimensionCuestionario(Long id, Long dimension, CondicionAplicacion condicion) {
        DimensionCuestionarioEntity d = new DimensionCuestionarioEntity();
        d.setId(id);
        d.setFkDimension(dimension);
        d.setFkCuestionario(1L);
        d.setFactorTransformacion(new BigDecimal("52"));
        d.setCondicionAplicacion(condicion);
        d.setMaxItemsSinRespuesta((short) 0);
        return d;
    }

    private static PreguntaEntity pregunta(Long id, Long dimensionCuestionario, int numero, Long escala) {
        PreguntaEntity p = new PreguntaEntity();
        p.setId(id);
        p.setFkDimensionCuestionario(dimensionCuestionario);
        p.setNumero(numero);
        p.setFkEscala(escala);
        p.setDescripcion("Pregunta " + numero);
        return p;
    }

    private static BaremoCuestionarioEntity baremoCuestionario(Long grupo, Long nivel, String minimo, String maximo) {
        BaremoCuestionarioEntity b = new BaremoCuestionarioEntity();
        b.setFkCuestionario(1L);
        b.setFkGrupoOcupacional(grupo);
        b.setFkNivelRiesgo(nivel);
        b.setValorMinimo(new BigDecimal(minimo));
        b.setValorMaximo(new BigDecimal(maximo));
        return b;
    }

    private static BaremoDominioEntity baremoDominio(Long dominioCuestionario, Long nivel, String minimo, String maximo) {
        BaremoDominioEntity b = new BaremoDominioEntity();
        b.setFkDominioCuestionario(dominioCuestionario);
        b.setFkGrupoOcupacional(1L);
        b.setFkNivelRiesgo(nivel);
        b.setValorMinimo(new BigDecimal(minimo));
        b.setValorMaximo(new BigDecimal(maximo));
        return b;
    }

    private static BaremoDimensionEntity baremoDimension(Long dimensionCuestionario, Long nivel, String minimo, String maximo) {
        BaremoDimensionEntity b = new BaremoDimensionEntity();
        b.setFkDimensionCuestionario(dimensionCuestionario);
        b.setFkGrupoOcupacional(1L);
        b.setFkNivelRiesgo(nivel);
        b.setValorMinimo(new BigDecimal(minimo));
        b.setValorMaximo(new BigDecimal(maximo));
        return b;
    }

    private static OpcionRespuestaEntity opcion(Long id, String nombre) {
        OpcionRespuestaEntity o = new OpcionRespuestaEntity();
        o.setId(id);
        o.setNombre(nombre);
        return o;
    }

    private static EscalaDetalleEntity escalaDetalle(Long escala, Long opcion, String valor) {
        EscalaDetalleEntity e = new EscalaDetalleEntity();
        e.setFkEscala(escala);
        e.setFkOpcionRespuesta(opcion);
        e.setValor(new BigDecimal(valor));
        return e;
    }

    private static EscalaEntity escala(Long id, String nombre) {
        EscalaEntity e = new EscalaEntity();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

}
