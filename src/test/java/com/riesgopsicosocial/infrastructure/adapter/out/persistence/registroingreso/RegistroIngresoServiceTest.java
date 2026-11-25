package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import com.riesgopsicosocial.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Rango de días (los dos incluidos), por defecto los últimos 30, totales de todo el rango y tope de filas. */
class RegistroIngresoServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 25);

    private final RegistroIngresoJpaRepository repository = mock(RegistroIngresoJpaRepository.class);
    private final RegistroIngresoService service = new RegistroIngresoService(repository,
            Clock.fixed(HOY.atTime(15, 0).atZone(BOGOTA).toInstant(), BOGOTA));

    private static IngresoFila fila(long id, ResultadoIngreso resultado) {
        return new IngresoFila(id, HOY.atTime(9, 0), "ana@prueba.co", 5L, "Ana", "190.1.2.3", "Mozilla/5.0", resultado);
    }

    @Test
    void sinFechas_losUltimos30DiasHastaHoyIncluido() {
        when(repository.contarPorResultado(any(), any())).thenReturn(List.of());
        when(repository.listar(any(), any(), any())).thenReturn(List.of());

        ConsultaIngresos consulta = service.consultar(null, null);

        assertEquals(LocalDate.of(2026, 10, 27), consulta.desde());
        assertEquals(HOY, consulta.hasta());
        LocalDateTime desde = LocalDate.of(2026, 10, 27).atStartOfDay();
        LocalDateTime finExclusivo = HOY.plusDays(1).atStartOfDay();
        verify(repository).contarPorResultado(desde, finExclusivo);
        verify(repository).listar(desde, finExclusivo, Limit.of(RegistroIngresoService.MAXIMO_FILAS));
    }

    @Test
    void totalesPorResultado_yNoTruncadoSiCabenTodas() {
        when(repository.contarPorResultado(any(), any())).thenReturn(List.of(
                new Object[]{ResultadoIngreso.EXITOSO, 1L}, new Object[]{ResultadoIngreso.FALLIDO, 1L}));
        when(repository.listar(any(), any(), any()))
                .thenReturn(List.of(fila(2, ResultadoIngreso.FALLIDO), fila(1, ResultadoIngreso.EXITOSO)));

        ConsultaIngresos consulta = service.consultar(LocalDate.of(2026, 11, 1), HOY);

        assertEquals(1, consulta.exitosos());
        assertEquals(1, consulta.fallidos());
        assertEquals(0, consulta.bloqueados(), "sin intentos de ese resultado = 0");
        assertFalse(consulta.truncado());
        assertEquals(2, consulta.filas().size());
    }

    @Test
    void masIntentosQueFilas_truncado_ylosTotalesCuentanTodoElRango() {
        when(repository.contarPorResultado(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ResultadoIngreso.FALLIDO, 2500L}));
        when(repository.listar(any(), any(), any())).thenReturn(List.of(fila(1, ResultadoIngreso.FALLIDO)));

        ConsultaIngresos consulta = service.consultar(null, null);

        assertTrue(consulta.truncado());
        assertEquals(2500, consulta.fallidos());
    }

    @Test
    void desdePosteriorAHasta_400SinConsultar() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.consultar(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10)));

        assertEquals("La fecha inicial (2026-11-20) es posterior a la final (2026-11-10)", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void soloDesde_llegaHastaHoy() {
        when(repository.contarPorResultado(any(), any())).thenReturn(List.of());
        when(repository.listar(any(), any(), any())).thenReturn(List.of());

        ConsultaIngresos consulta = service.consultar(LocalDate.of(2026, 11, 20), null);

        assertEquals(LocalDate.of(2026, 11, 20), consulta.desde());
        assertEquals(HOY, consulta.hasta());
    }
}
