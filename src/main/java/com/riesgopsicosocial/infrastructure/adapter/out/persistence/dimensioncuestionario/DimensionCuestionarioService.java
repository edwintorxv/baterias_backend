package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DimensionCuestionarioService {

    private final DimensionCuestionarioJpaRepository repository;

    public DimensionCuestionarioService(DimensionCuestionarioJpaRepository repository) {
        this.repository = repository;
    }

    public DimensionCuestionarioEntity crear(DimensionCuestionarioEntity entidad) {

        if (repository.existsByFkDimensionAndFkCuestionario(
                entidad.getFkDimension(), entidad.getFkCuestionario())) {
            throw new BusinessException(
                    "Ya existe una configuración de esta dimensión para este cuestionario.");
        }

        return repository.save(entidad);
    }

    public DimensionCuestionarioEntity actualizar(Long id, DimensionCuestionarioEntity datos) {
        DimensionCuestionarioEntity existente = obtenerPorId(id);

        boolean cambioRelacion = !existente.getFkDimension().equals(datos.getFkDimension())
                || !existente.getFkCuestionario().equals(datos.getFkCuestionario());

        if (cambioRelacion && repository.existsByFkDimensionAndFkCuestionario(
                datos.getFkDimension(), datos.getFkCuestionario())) {
            throw new BusinessException(
                    "Ya existe una configuración de esta dimensión para este cuestionario.");
        }

        existente.setFkDimension(datos.getFkDimension());
        existente.setFkCuestionario(datos.getFkCuestionario());
        existente.setFactorTransformacion(datos.getFactorTransformacion());

        return repository.save(existente);
    }

    public DimensionCuestionarioEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración dimensión-cuestionario no encontrada con id: " + id));
    }

    public List<DimensionCuestionarioEntity> listarTodos() {
        return repository.findAll();
    }

    public List<DimensionCuestionarioEntity> listarPorDimension(Long fkDimension) {
        return repository.findByFkDimension(fkDimension);
    }

    public List<DimensionCuestionarioEntity> listarPorCuestionario(Long fkCuestionario) {
        return repository.findByFkCuestionario(fkCuestionario);
    }

}
