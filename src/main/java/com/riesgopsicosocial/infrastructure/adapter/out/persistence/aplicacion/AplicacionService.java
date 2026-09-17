package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AplicacionService {

    private static final String ESTADO_DEFAULT = "FINALIZADA";

    private final AplicacionJpaRepository repository;

    public AplicacionService(AplicacionJpaRepository repository) {
        this.repository = repository;
    }

    public AplicacionEntity crear(AplicacionEntity entidad) {
        if (entidad.getFechaAplicacion() == null) {
            entidad.setFechaAplicacion(LocalDateTime.now());
        }
        if (entidad.getEstado() == null || entidad.getEstado().isBlank()) {
            entidad.setEstado(ESTADO_DEFAULT);
        }
        return repository.save(entidad);
    }

    public AplicacionEntity actualizar(Long id, AplicacionEntity datos) {
        AplicacionEntity existente = obtenerPorId(id);

        existente.setFkEvaluadoCliente(datos.getFkEvaluadoCliente());
        if (datos.getFechaAplicacion() != null) {
            existente.setFechaAplicacion(datos.getFechaAplicacion());
        }
        existente.setObservaciones(datos.getObservaciones());
        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            existente.setEstado(datos.getEstado());
        }

        return repository.save(existente);
    }

    public AplicacionEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada con id: " + id));
    }

    public List<AplicacionEntity> listarTodos() {
        return repository.findAll();
    }

    public List<AplicacionEntity> listarPorEvaluadoCliente(Long fkEvaluadoCliente) {
        return repository.findByFkEvaluadoCliente(fkEvaluadoCliente);
    }

    public List<AplicacionEntity> listarPorEstado(String estado) {
        return repository.findByEstado(estado);
    }


}
