package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de los intentos de ingreso (V22) para el administrador. Solo lectura: los escribe
 * {@code RegistroIngresos} en cada login y se borran solos a los 12 meses (la IP es un dato personal).
 */
@Service
public class RegistroIngresoService {

    /** Rango por defecto, en días, si no se indica. */
    static final int DIAS_POR_DEFECTO = 30;
    /** Tope de filas por consulta; los totales cuentan todo el rango. */
    public static final int MAXIMO_FILAS = 2000;

    private final RegistroIngresoJpaRepository repository;
    private final Clock reloj;

    @Autowired
    public RegistroIngresoService(RegistroIngresoJpaRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    RegistroIngresoService(RegistroIngresoJpaRepository repository, Clock reloj) {
        this.repository = repository;
        this.reloj = reloj;
    }

    /**
     * Intentos entre {@code desde} y {@code hasta}, los dos días incluidos. Sin {@code hasta}, hoy; sin
     * {@code desde}, los {@value #DIAS_POR_DEFECTO} días que terminan en {@code hasta}. 400 si desde es posterior.
     */
    @Transactional(readOnly = true)
    public ConsultaIngresos consultar(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(reloj);
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO - 1L);
        if (inicio.isAfter(fin)) {
            throw new ValidationException("La fecha inicial (" + inicio + ") es posterior a la final (" + fin + ")");
        }
        LocalDateTime desdeHora = inicio.atStartOfDay();
        LocalDateTime hastaHora = fin.plusDays(1).atStartOfDay();

        Map<ResultadoIngreso, Long> totales = new EnumMap<>(ResultadoIngreso.class);
        for (Object[] fila : repository.contarPorResultado(desdeHora, hastaHora)) {
            totales.put((ResultadoIngreso) fila[0], (Long) fila[1]);
        }
        long total = totales.values().stream().mapToLong(Long::longValue).sum();
        List<IngresoFila> filas = repository.listar(desdeHora, hastaHora, Limit.of(MAXIMO_FILAS));

        return new ConsultaIngresos(inicio, fin,
                totales.getOrDefault(ResultadoIngreso.EXITOSO, 0L),
                totales.getOrDefault(ResultadoIngreso.FALLIDO, 0L),
                totales.getOrDefault(ResultadoIngreso.BLOQUEADO, 0L),
                total > filas.size(), filas);
    }
}
