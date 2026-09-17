package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluadoClienteService {

    private final EvaluadoClienteJpaRepository repository;

    public EvaluadoClienteService(EvaluadoClienteJpaRepository repository) {
        this.repository = repository;
    }

    public EvaluadoClienteEntity crear(EvaluadoClienteEntity entidad) {

        if (entidad.getActivo() == null) {
            entidad.setActivo(true);
        }

        if (Boolean.TRUE.equals(entidad.getActivo())
                && repository.existsByFkEvaluadoAndFkClienteAndActivoTrue(
                entidad.getFkEvaluado(), entidad.getFkCliente())) {
            throw new BusinessException(
                    "El evaluado ya tiene una relación activa con este cliente. Actualice el registro existente en lugar de crear uno nuevo.");
        }

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
        existente.setActivo(quedaActiva);

        return repository.save(existente);
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

}
