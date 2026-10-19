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

    /**
     * Edita los datos o activa/inactiva. Una relación inactiva no se edita (conserva la foto con que se
     * evaluó): solo se puede activar. Crear: solo por {@link VinculacionPersonaService}.
     */
    public EvaluadoClienteEntity actualizar(Long id, EvaluadoClienteEntity datos) {
        EvaluadoClienteEntity existente = obtenerPorId(id);
        if (!Boolean.TRUE.equals(existente.getActivo()) && !Boolean.TRUE.equals(datos.getActivo())) {
            throw new BusinessException("La relación está inactiva y no se modifica: conserva los datos con que se "
                    + "evaluó. Si la persona volvió a la empresa, actívela primero.");
        }

        boolean cambioRelacion = !existente.getFkEvaluado().equals(datos.getFkEvaluado())
                || !existente.getFkCliente().equals(datos.getFkCliente());
        boolean quedaActiva = datos.getActivo() != null ? datos.getActivo() : existente.getActivo();
        boolean seActiva = !Boolean.TRUE.equals(existente.getActivo()) || cambioRelacion;

        if (Boolean.TRUE.equals(quedaActiva) && seActiva) {
            validarUnaSolaActiva(id, datos.getFkEvaluado(), datos.getFkCliente());
        }

        existente.setFkEvaluado(datos.getFkEvaluado());
        existente.setFkCliente(datos.getFkCliente());
        copiarFoto(datos, existente);
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
     * Datos laborales y datos de la persona que cambian: la foto de la vinculación. No toca persona,
     * cliente, vigencia ni auditoría.
     */
    static void copiarFoto(EvaluadoClienteEntity origen, EvaluadoClienteEntity destino) {
        destino.setFkCiudadTrabajo(origen.getFkCiudadTrabajo());
        destino.setAntiguedadEmpresa(origen.getAntiguedadEmpresa());
        destino.setNombreCargo(origen.getNombreCargo());
        destino.setFkTipoCargo(origen.getFkTipoCargo());
        destino.setFkTiempoCargo(origen.getFkTiempoCargo());
        destino.setNombreArea(origen.getNombreArea());
        destino.setFkTipoContrato(origen.getFkTipoContrato());
        destino.setFkHorasLabor(origen.getFkHorasLabor());
        destino.setFkTipoSalario(origen.getFkTipoSalario());
        destino.setFkEstadoCivil(origen.getFkEstadoCivil());
        destino.setFkNivelAcademico(origen.getFkNivelAcademico());
        destino.setFkCiudadResidencia(origen.getFkCiudadResidencia());
        destino.setFkEstratoSocioeconomico(origen.getFkEstratoSocioeconomico());
        destino.setFkTipoVivienda(origen.getFkTipoVivienda());
        destino.setOcupacionProfesion(origen.getOcupacionProfesion());
        destino.setFamiliaresDependientesEconomicamente(origen.getFamiliaresDependientesEconomicamente());
    }

    /**
     * Una persona solo tiene una relación activa. Para moverla de empresa se usa la vinculación, que
     * cierra la anterior; aquí solo se avisa (sin decir cuál es la otra empresa).
     */
    private void validarUnaSolaActiva(Long id, Long fkEvaluado, Long fkCliente) {
        for (EvaluadoClienteEntity activa : repository.findByFkEvaluadoAndActivoTrue(fkEvaluado)) {
            if (activa.getId().equals(id)) {
                continue;
            }
            throw new BusinessException(activa.getFkCliente().equals(fkCliente)
                    ? "El evaluado ya tiene una relación activa con este cliente. Actualice esa relación en lugar de activar otra."
                    : "El evaluado tiene una relación activa en otra empresa. Para cambiarlo de empresa, vincúlelo desde el cliente nuevo.");
        }
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
