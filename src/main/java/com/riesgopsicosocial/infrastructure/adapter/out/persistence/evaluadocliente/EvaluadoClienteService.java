package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class EvaluadoClienteService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final EvaluadoClienteJpaRepository repository;

    public EvaluadoClienteService(EvaluadoClienteJpaRepository repository) {
        this.repository = repository;
    }

    public EvaluadoClienteEntity crear(EvaluadoClienteEntity entidad) {

        if (entidad.getActivo() == null) {
            entidad.setActivo(true);
        }
        if (entidad.getFechaVinculacion() == null) {
            entidad.setFechaVinculacion(LocalDate.now(ZONA));
        }
        entidad.setFechaRetiro(fechaRetiro(entidad.getActivo(), entidad.getFechaRetiro(), null));
        validarVigencia(entidad);

        if (Boolean.TRUE.equals(entidad.getActivo())
                && repository.existsByFkEvaluadoAndFkClienteAndActivoTrue(
                entidad.getFkEvaluado(), entidad.getFkCliente())) {
            throw new BusinessException(
                    "El evaluado ya tiene una relación activa con este cliente. Actualice el registro existente en lugar de crear uno nuevo.");
        }

        entidad.setUsuarioCrea(UsuarioActual.nombre());
        return repository.save(entidad);
    }

    public EvaluadoClienteEntity actualizar(Long id, EvaluadoClienteEntity datos) {
        EvaluadoClienteEntity existente = obtenerPorId(id);

        boolean cambioRelacion = !existente.getFkEvaluado().equals(datos.getFkEvaluado())
                || !existente.getFkCliente().equals(datos.getFkCliente());
        boolean quedaActiva = datos.getActivo() != null ? datos.getActivo() : existente.getActivo();

        if (cambioRelacion && Boolean.TRUE.equals(quedaActiva)
                && repository.existsByFkEvaluadoAndFkClienteAndActivoTrue(
                datos.getFkEvaluado(), datos.getFkCliente())) {
            throw new BusinessException(
                    "El evaluado ya tiene una relación activa con este cliente. Actualice el registro existente en lugar de crear uno nuevo.");
        }

        existente.setFkEvaluado(datos.getFkEvaluado());
        existente.setFkCliente(datos.getFkCliente());
        existente.setFkCiudadTrabajo(datos.getFkCiudadTrabajo());
        existente.setAntiguedadEmpresa(datos.getAntiguedadEmpresa());
        existente.setNombreCargo(datos.getNombreCargo());
        existente.setFkTipoCargo(datos.getFkTipoCargo());
        existente.setFkTiempoCargo(datos.getFkTiempoCargo());
        existente.setNombreArea(datos.getNombreArea());
        existente.setFkTipoContrato(datos.getFkTipoContrato());
        existente.setFkHorasLabor(datos.getFkHorasLabor());
        existente.setFkTipoSalario(datos.getFkTipoSalario());
        existente.setFkEstadoCivil(datos.getFkEstadoCivil());
        existente.setFkNivelAcademico(datos.getFkNivelAcademico());
        existente.setFkCiudadResidencia(datos.getFkCiudadResidencia());
        existente.setFkEstratoSocioeconomico(datos.getFkEstratoSocioeconomico());
        existente.setFkTipoVivienda(datos.getFkTipoVivienda());
        existente.setOcupacionProfesion(datos.getOcupacionProfesion());
        existente.setFamiliaresDependientesEconomicamente(datos.getFamiliaresDependientesEconomicamente());
        if (datos.getFechaVinculacion() != null) {
            existente.setFechaVinculacion(datos.getFechaVinculacion());
        }
        existente.setFechaRetiro(fechaRetiro(quedaActiva, datos.getFechaRetiro(), existente.getFechaRetiro()));
        existente.setActivo(quedaActiva);
        validarVigencia(existente);

        existente.setFechaModificado(LocalDateTime.now());
        existente.setUsuarioModifica(UsuarioActual.nombre());
        return repository.save(existente);
    }

    /**
     * Activa: sin fecha de retiro (se reactiva). Inactiva: la que venga, si no la que ya tenía, si no hoy.
     */
    private static LocalDate fechaRetiro(Boolean activa, LocalDate solicitada, LocalDate anterior) {
        if (Boolean.TRUE.equals(activa)) {
            return null;
        }
        if (solicitada != null) {
            return solicitada;
        }
        return anterior != null ? anterior : LocalDate.now(ZONA);
    }

    private static void validarVigencia(EvaluadoClienteEntity relacion) {
        if (relacion.getFechaRetiro() != null && relacion.getFechaRetiro().isBefore(relacion.getFechaVinculacion())) {
            throw new BusinessException("La fecha de retiro no puede ser anterior a la fecha de vinculación ("
                    + relacion.getFechaVinculacion() + ")");
        }
    }

    public EvaluadoClienteEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relación evaluado-cliente no encontrada con id: " + id));
    }

    public EvaluadoClienteEntity obtenerActivaPorEvaluadoYCliente(Long fkEvaluado, Long fkCliente) {
        return repository.findByFkEvaluadoAndFkClienteAndActivoTrue(fkEvaluado, fkCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe relación activa entre el evaluado " + fkEvaluado + " y el cliente " + fkCliente));
    }

    public List<EvaluadoClienteEntity> listarTodos() {
        return repository.findAll();
    }

    public List<EvaluadoClienteEntity> listarPorEvaluado(Long fkEvaluado) {
        return repository.findByFkEvaluado(fkEvaluado);
    }

    public List<EvaluadoClienteEntity> listarPorCliente(Long fkCliente) {
        return repository.findByFkCliente(fkCliente);
    }

    public List<PersonaClienteFila> listarPersonasPorCliente(Long fkCliente) {
        return repository.listarPersonasPorCliente(fkCliente);
    }

}
