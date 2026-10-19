package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvaluadoClienteServiceTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

    private final EvaluadoClienteJpaRepository repository = mock(EvaluadoClienteJpaRepository.class);
    private final EvaluadoClienteService service = new EvaluadoClienteService(repository);

    @BeforeEach
    void repositorio() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.findByFkEvaluadoAndActivoTrue(any())).thenReturn(List.of());
    }

    @Test
    void inactivar_poneLaFechaDeRetiroDeHoySiNoViene() {
        existe(relacion(7L, 1L, true, null));

        EvaluadoClienteEntity actualizada = service.actualizar(7L, datos(1L, false, null));

        assertFalse(actualizada.getActivo());
        assertEquals(HOY, actualizada.getFechaRetiro());
    }

    @Test
    void reactivar_borraLaFechaDeRetiro() {
        existe(relacion(7L, 1L, false, LocalDate.of(2025, 6, 1)));

        EvaluadoClienteEntity actualizada = service.actualizar(7L, datos(1L, true, null));

        assertTrue(actualizada.getActivo());
        assertNull(actualizada.getFechaRetiro());
    }

    @Test
    void reactivarEstandoActivaEnOtraEmpresa_409() {
        existe(relacion(7L, 1L, false, LocalDate.of(2025, 6, 1)));
        when(repository.findByFkEvaluadoAndActivoTrue(50L)).thenReturn(List.of(relacion(8L, 2L, true, null)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.actualizar(7L, datos(1L, true, null)));
        assertTrue(error.getMessage().contains("otra empresa"));
        verify(repository, never()).save(any());
    }

    @Test
    void editarUnaActiva_noChocaConsigoMisma() {
        EvaluadoClienteEntity activa = relacion(7L, 1L, true, null);
        existe(activa);
        when(repository.findByFkEvaluadoAndActivoTrue(50L)).thenReturn(List.of(activa));

        EvaluadoClienteEntity datos = datos(1L, true, null);
        datos.setNombreArea("Producción");

        assertEquals("Producción", service.actualizar(7L, datos).getNombreArea());
    }

    @Test
    void relacionInactiva_noSeEditaSoloSeActiva() {
        existe(relacion(7L, 1L, false, LocalDate.of(2025, 6, 1)));

        EvaluadoClienteEntity datos = datos(1L, false, null);
        datos.setNombreArea("Otra área");
        BusinessException error = assertThrows(BusinessException.class, () -> service.actualizar(7L, datos));
        assertTrue(error.getMessage().contains("inactiva"));

        EvaluadoClienteEntity sinActivo = datos(1L, false, null);
        sinActivo.setActivo(null);
        assertThrows(BusinessException.class, () -> service.actualizar(7L, sinActivo));
        verify(repository, never()).save(any());
    }

    @Test
    void retiroAnteriorALaVinculacion_409() {
        existe(relacion(7L, 1L, true, null));

        assertThrows(BusinessException.class, () -> service.actualizar(7L, datos(1L, false, LocalDate.of(2024, 12, 31))));
    }

    private void existe(EvaluadoClienteEntity relacion) {
        when(repository.findById(relacion.getId())).thenReturn(Optional.of(relacion));
    }

    private static EvaluadoClienteEntity relacion(Long id, Long cliente, boolean activa, LocalDate retiro) {
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setId(id);
        relacion.setFkEvaluado(50L);
        relacion.setFkCliente(cliente);
        relacion.setActivo(activa);
        relacion.setFechaVinculacion(LocalDate.of(2025, 1, 15));
        relacion.setFechaRetiro(retiro);
        return relacion;
    }

    private static EvaluadoClienteEntity datos(Long cliente, boolean activa, LocalDate retiro) {
        EvaluadoClienteEntity datos = new EvaluadoClienteEntity();
        datos.setFkEvaluado(50L);
        datos.setFkCliente(cliente);
        datos.setActivo(activa);
        datos.setFechaRetiro(retiro);
        return datos;
    }

}
