package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Única forma de crear personas y vínculos con un cliente (formulario o archivo). Reglas:
 * <ul>
 *   <li>Una persona por documento; si ya existe, ganan los datos nuevos (siempre se recapturan).</li>
 *   <li>Una sola empresa activa a la vez: si estaba activa en otra, esa relación se cierra (con la
 *       foto con que se evaluó) y se abre la nueva.</li>
 *   <li>Si ya estaba activa en este cliente, se actualiza la foto de esa relación.</li>
 * </ul>
 */
@Service
public class VinculacionPersonaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    static final int EDAD_MINIMA = 14;
    static final int EDAD_MAXIMA = 90;

    private final EvaluadoJpaRepository evaluadoRepository;
    private final EvaluadoClienteJpaRepository relacionRepository;
    private final ClienteJpaRepository clienteRepository;

    public VinculacionPersonaService(EvaluadoJpaRepository evaluadoRepository,
                                     EvaluadoClienteJpaRepository relacionRepository,
                                     ClienteJpaRepository clienteRepository) {
        this.evaluadoRepository = evaluadoRepository;
        this.relacionRepository = relacionRepository;
        this.clienteRepository = clienteRepository;
    }

    /** Qué pasaría al vincular ese documento al cliente, sin escribir nada (vista previa de la carga). */
    @Transactional(readOnly = true)
    public ResultadoVinculacion clasificar(Long idCliente, String numeroIdentificacion) {
        return clasificar(idCliente, evaluadoRepository.findByNumeroIdentificacion(numeroIdentificacion.trim()));
    }

    /**
     * @param persona  documento, nombre, apellido, sexo y año de nacimiento
     * @param relacion datos laborales y datos de la persona que cambian (la foto de esta vinculación);
     *                 se ignoran fkEvaluado, fkCliente, activo y fecha de retiro
     */
    @Transactional
    public Vinculacion vincular(Long idCliente, EvaluadoEntity persona, EvaluadoClienteEntity relacion) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
        }
        validarAnioNacimiento(persona.getAnioNacimiento());

        Optional<EvaluadoEntity> existente = evaluadoRepository.findByNumeroIdentificacion(
                persona.getNumeroIdentificacion().trim());
        ResultadoVinculacion resultado = clasificar(idCliente, existente);
        boolean datosPersonaActualizados = existente.isPresent() && cambiaIdentidad(existente.get(), persona);
        EvaluadoEntity evaluado = guardarPersona(existente, persona);

        if (resultado == ResultadoVinculacion.ACTUALIZA) {
            EvaluadoClienteEntity activa = activas(evaluado.getId()).stream()
                    .filter(r -> r.getFkCliente().equals(idCliente))
                    .findFirst()
                    .orElseThrow();
            EvaluadoClienteService.copiarFoto(relacion, activa);
            marcarModificado(activa);
            return new Vinculacion(resultado, evaluado, relacionRepository.save(activa), datosPersonaActualizados);
        }

        LocalDate vinculacion = relacion.getFechaVinculacion() != null ? relacion.getFechaVinculacion() : LocalDate.now(ZONA);
        if (resultado == ResultadoVinculacion.CAMBIA_EMPRESA) {
            cerrarEnOtrasEmpresas(evaluado.getId(), vinculacion);
        }

        EvaluadoClienteEntity nueva = new EvaluadoClienteEntity();
        nueva.setFkEvaluado(evaluado.getId());
        nueva.setFkCliente(idCliente);
        EvaluadoClienteService.copiarFoto(relacion, nueva);
        nueva.setFechaVinculacion(vinculacion);
        nueva.setFechaRetiro(null);
        nueva.setActivo(true);
        nueva.setUsuarioCrea(UsuarioActual.nombre());
        return new Vinculacion(resultado, evaluado, relacionRepository.save(nueva), datosPersonaActualizados);
    }

    private ResultadoVinculacion clasificar(Long idCliente, Optional<EvaluadoEntity> existente) {
        if (existente.isEmpty()) {
            return ResultadoVinculacion.NUEVA;
        }
        List<EvaluadoClienteEntity> activas = activas(existente.get().getId());
        if (activas.isEmpty()) {
            return ResultadoVinculacion.VINCULA;
        }
        return activas.stream().anyMatch(r -> r.getFkCliente().equals(idCliente))
                ? ResultadoVinculacion.ACTUALIZA
                : ResultadoVinculacion.CAMBIA_EMPRESA;
    }

    private List<EvaluadoClienteEntity> activas(Long idEvaluado) {
        return relacionRepository.findByFkEvaluadoAndActivoTrue(idEvaluado);
    }

    /** La relación cerrada no se toca más que para cerrarla: conserva la foto con que se evaluó. */
    private void cerrarEnOtrasEmpresas(Long idEvaluado, LocalDate retiro) {
        for (EvaluadoClienteEntity anterior : activas(idEvaluado)) {
            if (retiro.isBefore(anterior.getFechaVinculacion())) {
                throw new BusinessException("La fecha de vinculación (" + retiro + ") es anterior a la de su "
                        + "vínculo activo en otra empresa (" + anterior.getFechaVinculacion() + ")");
            }
            anterior.setActivo(false);
            anterior.setFechaRetiro(retiro);
            marcarModificado(anterior);
            relacionRepository.save(anterior);
        }
    }

    /** Nueva: se crea. Existente: ganan los datos que llegan (documento igual, se recapturan los demás). */
    private EvaluadoEntity guardarPersona(Optional<EvaluadoEntity> existente, EvaluadoEntity datos) {
        EvaluadoEntity persona = existente.orElseGet(EvaluadoEntity::new);
        if (existente.isEmpty()) {
            persona.setNumeroIdentificacion(datos.getNumeroIdentificacion().trim());
            persona.setUsuarioCrea(UsuarioActual.nombre());
        } else if (!cambiaIdentidad(persona, datos)) {
            return persona;
        } else {
            persona.setFechaModificado(LocalDateTime.now());
            persona.setUsuarioModifica(UsuarioActual.nombre());
        }
        persona.setNombre(datos.getNombre().trim());
        persona.setApellido(datos.getApellido().trim());
        persona.setFkSexo(datos.getFkSexo());
        persona.setAnioNacimiento(datos.getAnioNacimiento());
        return evaluadoRepository.save(persona);
    }

    private static boolean cambiaIdentidad(EvaluadoEntity actual, EvaluadoEntity datos) {
        return !actual.getNombre().equals(datos.getNombre().trim())
                || !actual.getApellido().equals(datos.getApellido().trim())
                || !Objects.equals(actual.getFkSexo(), datos.getFkSexo())
                || !Objects.equals(actual.getAnioNacimiento(), datos.getAnioNacimiento());
    }

    /** Edad entre 14 y 90 años en el año actual. */
    static void validarAnioNacimiento(Integer anio) {
        int actual = LocalDate.now(ZONA).getYear();
        if (anio == null || anio > actual - EDAD_MINIMA || anio < actual - EDAD_MAXIMA) {
            throw new BusinessException("El año de nacimiento debe estar entre " + (actual - EDAD_MAXIMA) + " y "
                    + (actual - EDAD_MINIMA) + " (edad de " + EDAD_MINIMA + " a " + EDAD_MAXIMA + " años)");
        }
    }

    private static void marcarModificado(EvaluadoClienteEntity relacion) {
        relacion.setFechaModificado(LocalDateTime.now());
        relacion.setUsuarioModifica(UsuarioActual.nombre());
    }

}
