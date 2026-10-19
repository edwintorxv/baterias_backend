package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VinculacionPersonaServiceTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
    private static final int ANIO_VALIDO = HOY.getYear() - 30;

    private final EvaluadoJpaRepository evaluadoRepository = mock(EvaluadoJpaRepository.class);
    private final EvaluadoClienteJpaRepository relacionRepository = mock(EvaluadoClienteJpaRepository.class);
    private final ClienteJpaRepository clienteRepository = mock(ClienteJpaRepository.class);
    private final VinculacionPersonaService service = new VinculacionPersonaService(evaluadoRepository,
            relacionRepository, clienteRepository);

    /** Lo que se guarda, en orden. */
    private final List<EvaluadoClienteEntity> relacionesGuardadas = new ArrayList<>();

    @BeforeEach
    void repositorios() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(evaluadoRepository.findByNumeroIdentificacion(any())).thenReturn(Optional.empty());
        when(relacionRepository.findByFkEvaluadoAndActivoTrue(any())).thenReturn(List.of());
        when(evaluadoRepository.save(any())).thenAnswer(i -> {
            EvaluadoEntity persona = i.getArgument(0);
            if (persona.getId() == null) {
                persona.setId(50L);
            }
            return persona;
        });
        when(relacionRepository.save(any())).thenAnswer(i -> {
            EvaluadoClienteEntity relacion = i.getArgument(0);
            relacionesGuardadas.add(relacion);
            return relacion;
        });
    }

    @Test
    void documentoNuevo_creaPersonaYRelacionActivaDesdeHoy() {
        Vinculacion vinculacion = service.vincular(1L, persona(" 1010 ", "Ana"), foto("Ventas"));

        assertEquals(ResultadoVinculacion.NUEVA, vinculacion.resultado());
        assertEquals("1010", vinculacion.evaluado().getNumeroIdentificacion());
        assertFalse(vinculacion.datosPersonaActualizados());
        EvaluadoClienteEntity relacion = vinculacion.relacion();
        assertEquals(50L, relacion.getFkEvaluado());
        assertEquals(1L, relacion.getFkCliente());
        assertTrue(relacion.getActivo());
        assertEquals(HOY, relacion.getFechaVinculacion());
        assertNull(relacion.getFechaRetiro());
        assertEquals("Ventas", relacion.getNombreArea());
        assertEquals(4L, relacion.getFkNivelAcademico());
    }

    @Test
    void personaSinVinculoActivo_soloCreaLaRelacionYNoTocaSusDatosSiSonIguales() {
        existe(persona("1010", "Ana"));

        Vinculacion vinculacion = service.vincular(1L, persona("1010", "Ana"), foto("Ventas"));

        assertEquals(ResultadoVinculacion.VINCULA, vinculacion.resultado());
        assertFalse(vinculacion.datosPersonaActualizados());
        verify(evaluadoRepository, never()).save(any());
        assertEquals(1, relacionesGuardadas.size());
    }

    @Test
    void personaExistente_gananLosDatosNuevos() {
        existe(persona("1010", "Ana"));

        Vinculacion vinculacion = service.vincular(1L, persona("1010", "Ana María"), foto("Ventas"));

        assertTrue(vinculacion.datosPersonaActualizados());
        assertEquals("Ana María", vinculacion.evaluado().getNombre());
        assertNotNull(vinculacion.evaluado().getUsuarioModifica());
    }

    @Test
    void yaActivaEnEsteCliente_actualizaLaFotoYConservaLaFechaDeVinculacion() {
        existe(persona("1010", "Ana"));
        EvaluadoClienteEntity activa = relacion(7L, 1L, LocalDate.of(2025, 2, 1));
        when(relacionRepository.findByFkEvaluadoAndActivoTrue(50L)).thenReturn(List.of(activa));

        EvaluadoClienteEntity datos = foto("Producción");
        datos.setFechaVinculacion(HOY);
        Vinculacion vinculacion = service.vincular(1L, persona("1010", "Ana"), datos);

        assertEquals(ResultadoVinculacion.ACTUALIZA, vinculacion.resultado());
        assertSame(activa, vinculacion.relacion());
        assertEquals("Producción", activa.getNombreArea());
        assertEquals(LocalDate.of(2025, 2, 1), activa.getFechaVinculacion());
        assertTrue(activa.getActivo());
        assertEquals(1, relacionesGuardadas.size(), "no crea otra relación");
    }

    @Test
    void activaEnOtraEmpresa_cierraEsaConSuFotoYCreaLaNueva() {
        existe(persona("1010", "Ana"));
        EvaluadoClienteEntity anterior = relacion(7L, 2L, LocalDate.of(2025, 2, 1));
        anterior.setNombreArea("Bodega");
        when(relacionRepository.findByFkEvaluadoAndActivoTrue(50L)).thenReturn(List.of(anterior));

        EvaluadoClienteEntity datos = foto("Ventas");
        datos.setFechaVinculacion(LocalDate.of(2026, 3, 1));
        Vinculacion vinculacion = service.vincular(1L, persona("1010", "Ana"), datos);

        assertEquals(ResultadoVinculacion.CAMBIA_EMPRESA, vinculacion.resultado());
        assertFalse(anterior.getActivo());
        assertEquals(LocalDate.of(2026, 3, 1), anterior.getFechaRetiro());
        assertEquals("Bodega", anterior.getNombreArea(), "la relación cerrada conserva su foto");
        assertEquals(2, relacionesGuardadas.size());
        assertEquals(1L, vinculacion.relacion().getFkCliente());
        assertEquals(LocalDate.of(2026, 3, 1), vinculacion.relacion().getFechaVinculacion());
    }

    @Test
    void cambioDeEmpresaConFechaAnteriorAlVinculoActivo_409SinGuardar() {
        existe(persona("1010", "Ana"));
        when(relacionRepository.findByFkEvaluadoAndActivoTrue(50L))
                .thenReturn(List.of(relacion(7L, 2L, LocalDate.of(2026, 5, 1))));

        EvaluadoClienteEntity datos = foto("Ventas");
        datos.setFechaVinculacion(LocalDate.of(2026, 4, 1));

        assertThrows(BusinessException.class, () -> service.vincular(1L, persona("1010", "Ana"), datos));
        assertTrue(relacionesGuardadas.isEmpty());
    }

    @Test
    void clienteInexistente_404() {
        assertThrows(ResourceNotFoundException.class, () -> service.vincular(9L, persona("1010", "Ana"), foto("Ventas")));
    }

    @Test
    void anioDeNacimientoFueraDeRango_409() {
        EvaluadoEntity menor = persona("1010", "Ana");
        menor.setAnioNacimiento(HOY.getYear() - 10);
        EvaluadoEntity mayor = persona("1010", "Ana");
        mayor.setAnioNacimiento(HOY.getYear() - 95);

        assertThrows(BusinessException.class, () -> service.vincular(1L, menor, foto("Ventas")));
        assertThrows(BusinessException.class, () -> service.vincular(1L, mayor, foto("Ventas")));
        verify(evaluadoRepository, never()).save(any());
    }

    @Test
    void clasificar_diceQuePasariaSinEscribir() {
        assertEquals(ResultadoVinculacion.NUEVA, service.clasificar(1L, "1010"));

        existe(persona("1010", "Ana"));
        assertEquals(ResultadoVinculacion.VINCULA, service.clasificar(1L, "1010"));

        when(relacionRepository.findByFkEvaluadoAndActivoTrue(50L))
                .thenReturn(List.of(relacion(7L, 2L, LocalDate.of(2025, 2, 1))));
        assertEquals(ResultadoVinculacion.CAMBIA_EMPRESA, service.clasificar(1L, "1010"));
        assertEquals(ResultadoVinculacion.ACTUALIZA, service.clasificar(2L, "1010"));

        verify(evaluadoRepository, never()).save(any());
        verify(relacionRepository, never()).save(any());
    }

    private void existe(EvaluadoEntity persona) {
        persona.setId(50L);
        when(evaluadoRepository.findByNumeroIdentificacion(persona.getNumeroIdentificacion())).thenReturn(Optional.of(persona));
    }

    private static EvaluadoEntity persona(String documento, String nombre) {
        EvaluadoEntity persona = new EvaluadoEntity();
        persona.setNumeroIdentificacion(documento);
        persona.setNombre(nombre);
        persona.setApellido("Pérez");
        persona.setFkSexo(2L);
        persona.setAnioNacimiento(ANIO_VALIDO);
        return persona;
    }

    private static EvaluadoClienteEntity foto(String area) {
        EvaluadoClienteEntity datos = new EvaluadoClienteEntity();
        datos.setNombreArea(area);
        datos.setFkTipoCargo(3L);
        datos.setFkNivelAcademico(4L);
        return datos;
    }

    private static EvaluadoClienteEntity relacion(Long id, Long cliente, LocalDate vinculacion) {
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setId(id);
        relacion.setFkEvaluado(50L);
        relacion.setFkCliente(cliente);
        relacion.setActivo(true);
        relacion.setFechaVinculacion(vinculacion);
        return relacion;
    }

}
