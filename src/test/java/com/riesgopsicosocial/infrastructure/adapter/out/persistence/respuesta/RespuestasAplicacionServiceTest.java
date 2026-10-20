package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado.BorradoResultados;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestasAplicacionService.Guardado;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestasAplicacionService.RespuestaNueva;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Cuestionarios de prueba: 1 (A, grupo 1), 2 (B, grupo 2) y 4 (D, ambos). En A, la pregunta 1 siempre
 * aplica y la 2 depende de "atiende clientes"; D tiene una pregunta con escala de 4 opciones.
 */
class RespuestasAplicacionServiceTest {

    private final AplicacionJpaRepository aplicacionRepository = mock(AplicacionJpaRepository.class);
    private final RespuestaJpaRepository respuestaRepository = mock(RespuestaJpaRepository.class);
    private final CuestionarioJpaRepository cuestionarioRepository = mock(CuestionarioJpaRepository.class);
    private final BaremoCuestionarioJpaRepository baremoRepository = mock(BaremoCuestionarioJpaRepository.class);
    private final DimensionCuestionarioJpaRepository dimensionRepository = mock(DimensionCuestionarioJpaRepository.class);
    private final PreguntaJpaRepository preguntaRepository = mock(PreguntaJpaRepository.class);
    private final EscalaDetalleJpaRepository escalaDetalleRepository = mock(EscalaDetalleJpaRepository.class);
    private final OpcionRespuestaJpaRepository opcionRepository = mock(OpcionRespuestaJpaRepository.class);
    private final BorradoResultados borradoResultados = mock(BorradoResultados.class);
    private final RespuestasAplicacionService service = new RespuestasAplicacionService(aplicacionRepository,
            respuestaRepository, cuestionarioRepository, baremoRepository, dimensionRepository, preguntaRepository,
            escalaDetalleRepository, opcionRepository, borradoResultados);

    private final AplicacionEntity aplicacion = new AplicacionEntity();
    private final List<RespuestaEntity> guardadas = new ArrayList<>();

    @BeforeEach
    void configuracion() {
        aplicacion.setId(9L);
        aplicacion.setFkGrupoOcupacional(1L);
        when(aplicacionRepository.findById(9L)).thenReturn(Optional.of(aplicacion));
        when(cuestionarioRepository.findAll()).thenReturn(List.of(cuestionario(1L, "A"), cuestionario(2L, "B"), cuestionario(4L, "D")));
        when(baremoRepository.findByFkGrupoOcupacional(1L)).thenReturn(List.of(baremo(1L), baremo(4L)));

        List<DimensionCuestionarioEntity> dimensiones = List.of(dimension(10L, 1L, null),
                dimension(11L, 1L, CondicionAplicacion.ATIENDE_CLIENTES), dimension(20L, 2L, null), dimension(40L, 4L, null));
        when(dimensionRepository.findAll()).thenReturn(dimensiones);
        when(dimensionRepository.findByFkCuestionario(any())).thenAnswer(i -> dimensiones.stream()
                .filter(d -> d.getFkCuestionario().equals(i.getArgument(0))).toList());

        List<PreguntaEntity> preguntas = List.of(new PreguntaEntity(100L, 10L, 1L, 1, "Pregunta A1"),
                new PreguntaEntity(101L, 11L, 1L, 2, "Pregunta A2 (clientes)"),
                new PreguntaEntity(200L, 20L, 1L, 1, "Pregunta B1"),
                new PreguntaEntity(400L, 40L, 3L, 1, "Pregunta D1"));
        when(preguntaRepository.findByFkDimensionCuestionarioIn(anyCollection())).thenAnswer(i -> preguntas.stream()
                .filter(p -> i.<java.util.Collection<Long>>getArgument(0).contains(p.getFkDimensionCuestionario())).toList());

        List<EscalaDetalleEntity> escalas = new ArrayList<>();
        for (long opcion = 1; opcion <= 5; opcion++) {
            escalas.add(new EscalaDetalleEntity(null, 1L, opcion, BigDecimal.valueOf(opcion - 1)));
        }
        for (long opcion : new long[]{1, 2, 3, 5}) {
            escalas.add(new EscalaDetalleEntity(null, 3L, opcion, BigDecimal.valueOf(opcion * 3)));
        }
        when(escalaDetalleRepository.findAll()).thenReturn(escalas);
        when(opcionRepository.findAll()).thenReturn(List.of(opcion(1L, "Siempre"), opcion(2L, "Casi siempre"),
                opcion(3L, "Algunas veces"), opcion(4L, "Casi nunca"), opcion(5L, "Nunca")));

        when(respuestaRepository.findByFkAplicacion(9L)).thenAnswer(i -> List.copyOf(guardadas));
    }

    @Test
    void formulario_soloLosCuestionariosDelGrupo_conOpcionesDeSuEscala() {
        FormularioAplicacion formulario = service.formulario(9L);

        assertEquals(List.of("A", "D"), formulario.cuestionarios().stream().map(FormularioAplicacion.Cuestionario::forma).toList());
        FormularioAplicacion.Pregunta d1 = formulario.cuestionarios().get(1).preguntas().get(0);
        assertEquals(List.of("Siempre", "Casi siempre", "Algunas veces", "Nunca"),
                d1.opciones().stream().map(FormularioAplicacion.Opcion::nombre).toList());
        assertEquals(EstadoAplicacion.SIN_RESPUESTAS, formulario.estado());
    }

    @Test
    void formulario_preguntaFiltroEnNo_laPreguntaNoAplicaNiCuenta() {
        aplicacion.setAtiendeClientes(false);

        FormularioAplicacion.Cuestionario a = service.formulario(9L).cuestionarios().get(0);

        assertEquals("ATIENDE_CLIENTES", a.preguntas().get(1).condicion());
        assertFalse(a.preguntas().get(1).aplica());
        assertEquals(1, a.aplicables());
    }

    @Test
    void formulario_estadoSegunRespuestasYResultados() {
        aplicacion.setAtiendeClientes(false);
        guardadas.add(respuesta(100L, 1L));
        assertEquals(EstadoAplicacion.INCOMPLETA, service.formulario(9L).estado(), "falta D");

        guardadas.add(respuesta(400L, 5L));
        assertEquals(EstadoAplicacion.PENDIENTE_CALCULO, service.formulario(9L).estado());

        when(borradoResultados.tieneResultados(9L)).thenReturn(true);
        assertEquals(EstadoAplicacion.CALCULADA, service.formulario(9L).estado());
    }

    @Test
    void guardar_reemplazaLasDelCuestionarioConSuValorYBorraResultados() {
        guardadas.add(respuesta(100L, 1L));
        guardadas.add(respuesta(400L, 5L));
        when(borradoResultados.borrar(9L)).thenReturn(true);

        Guardado guardado = service.guardar(9L, 1L, List.of(new RespuestaNueva(100L, 3L), new RespuestaNueva(101L, 5L)));

        assertTrue(guardado.huboCambios());
        assertTrue(guardado.resultadosBorrados());
        verify(respuestaRepository).deleteAllInBatch(List.of(guardadas.get(0)));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RespuestaEntity>> nuevas = ArgumentCaptor.forClass(List.class);
        verify(respuestaRepository).saveAll(nuevas.capture());
        assertEquals(List.of(new BigDecimal("2"), new BigDecimal("4")),
                nuevas.getValue().stream().map(RespuestaEntity::getValorObtenido).toList(), "valor de la escala");
        verify(aplicacionRepository).save(aplicacion);
    }

    @Test
    void guardar_sinCambios_noTocaNadaNiBorraResultados() {
        guardadas.add(respuesta(100L, 1L));

        Guardado guardado = service.guardar(9L, 1L, List.of(new RespuestaNueva(100L, 1L)));

        assertFalse(guardado.huboCambios());
        verify(respuestaRepository, never()).deleteAllInBatch(any());
        verify(borradoResultados, never()).borrar(any());
    }

    @Test
    void guardar_unErrorPorPregunta_yNoGuardaNada() {
        aplicacion.setAtiendeClientes(false);

        ValidationException error = assertThrows(ValidationException.class, () -> service.guardar(9L, 1L, List.of(
                new RespuestaNueva(100L, 1L), new RespuestaNueva(100L, 2L),
                new RespuestaNueva(101L, 1L),
                new RespuestaNueva(400L, 1L))));

        assertEquals(List.of("Pregunta 1: viene más de una vez",
                        "Pregunta 2: no aplica (la pregunta filtro de la aplicación es \"no\")",
                        "La pregunta 400 no es del cuestionario A"),
                error.getDetails());
        verify(respuestaRepository, never()).saveAll(any());
    }

    @Test
    void guardar_opcionFueraDeLaEscala_400() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.guardar(9L, 4L, List.of(new RespuestaNueva(400L, 4L))));

        assertEquals(List.of("Pregunta 1: la opción 4 no es de su escala"), error.getDetails());
    }

    @Test
    void guardar_cuestionarioDeOtroGrupo_409() {
        assertThrows(BusinessException.class, () -> service.guardar(9L, 2L, List.of(new RespuestaNueva(200L, 1L))));
    }

    private static CuestionarioEntity cuestionario(Long id, String forma) {
        CuestionarioEntity cuestionario = new CuestionarioEntity();
        cuestionario.setId(id);
        cuestionario.setForma(forma);
        cuestionario.setNombre("Cuestionario " + forma);
        return cuestionario;
    }

    private static BaremoCuestionarioEntity baremo(Long idCuestionario) {
        BaremoCuestionarioEntity baremo = new BaremoCuestionarioEntity();
        baremo.setFkCuestionario(idCuestionario);
        baremo.setFkGrupoOcupacional(1L);
        return baremo;
    }

    private static DimensionCuestionarioEntity dimension(Long id, Long idCuestionario, CondicionAplicacion condicion) {
        DimensionCuestionarioEntity dimension = new DimensionCuestionarioEntity();
        dimension.setId(id);
        dimension.setFkCuestionario(idCuestionario);
        dimension.setCondicionAplicacion(condicion);
        return dimension;
    }

    private static OpcionRespuestaEntity opcion(Long id, String nombre) {
        OpcionRespuestaEntity opcion = new OpcionRespuestaEntity();
        opcion.setId(id);
        opcion.setNombre(nombre);
        return opcion;
    }

    private static RespuestaEntity respuesta(Long idPregunta, Long idOpcion) {
        RespuestaEntity respuesta = new RespuestaEntity();
        respuesta.setFkAplicacion(9L);
        respuesta.setFkPregunta(idPregunta);
        respuesta.setFkOpcionRespuesta(idOpcion);
        return respuesta;
    }

}
