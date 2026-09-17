package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DominioCuestionarioService {

    private final DominioCuestionarioJpaRepository repository;

    public DominioCuestionarioService(DominioCuestionarioJpaRepository repository) {
        this.repository = repository;
    }

    public DominioCuestionarioEntity crear(DominioCuestionarioEntity entidad) {

        if (repository.existsByFkDominioAndFkCuestionario(
                entidad.getFkDominio(), entidad.getFkCuestionario())) {
            throw new BusinessException(
                    "Ya existe una configuración de este dominio para este cuestionario.");
        }

        return repository.save(entidad);
    }

    public DominioCuestionarioEntity actualizar(Long id, DominioCuestionarioEntity datos) {
        DominioCuestionarioEntity existente = obtenerPorId(id);

        boolean cambioRelacion = !existente.getFkDominio().equals(datos.getFkDominio())
                || !existente.getFkCuestionario().equals(datos.getFkCuestionario());

        if (cambioRelacion && repository.existsByFkDominioAndFkCuestionario(
                datos.getFkDominio(), datos.getFkCuestionario())) {
            throw new BusinessException(
                    "Ya existe una configuración de este dominio para este cuestionario.");
        }

        existente.setFkDominio(datos.getFkDominio());
        existente.setFkCuestionario(datos.getFkCuestionario());
        existente.setFactorTransformacion(datos.getFactorTransformacion());

        return repository.save(existente);
    }

    public DominioCuestionarioEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración dominio-cuestionario no encontrada con id: " + id));
    }

    public List<DominioCuestionarioEntity> listarTodos() {
        return repository.findAll();
    }

    public List<DominioCuestionarioEntity> listarPorDominio(Long fkDominio) {
        return repository.findByFkDominio(fkDominio);
    }

    public List<DominioCuestionarioEntity> listarPorCuestionario(Long fkCuestionario) {
        return repository.findByFkCuestionario(fkCuestionario);
    }

}
