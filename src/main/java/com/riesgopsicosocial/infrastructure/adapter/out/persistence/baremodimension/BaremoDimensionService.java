package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension;

import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaremoDimensionService {

    private final BaremoDimensionJpaRepository repository;

    public BaremoDimensionService(BaremoDimensionJpaRepository repository) {
        this.repository = repository;
    }

    public BaremoDimensionEntity crear(BaremoDimensionEntity entidad) {
        return repository.save(entidad);
    }

    public BaremoDimensionEntity actualizar(Long id, BaremoDimensionEntity datos) {
        BaremoDimensionEntity existente = obtenerPorId(id);

        existente.setFkDimensionCuestionario(datos.getFkDimensionCuestionario());
        existente.setFkNivelRiesgo(datos.getFkNivelRiesgo());
        existente.setValorMinimo(datos.getValorMinimo());
        existente.setValorMaximo(datos.getValorMaximo());

        return repository.save(existente);
    }

    public BaremoDimensionEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Baremo de dimensión no encontrado con id: " + id));
    }

    public List<BaremoDimensionEntity> listarTodos() {
        return repository.findAll();
    }

    public List<BaremoDimensionEntity> listarPorDimensionCuestionario(Long fkDimensionCuestionario) {
        return repository.findByFkDimensionCuestionario(fkDimensionCuestionario);
    }

    public List<BaremoDimensionEntity> listarPorNivelRiesgo(Long fkNivelRiesgo) {
        return repository.findByFkNivelRiesgo(fkNivelRiesgo);
    }

}
