package com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta;

import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PreguntaService {

    private final PreguntaJpaRepository repository;

    public PreguntaService(PreguntaJpaRepository repository) {
        this.repository = repository;
    }

    public PreguntaEntity crear(PreguntaEntity entidad) {
        return repository.save(entidad);
    }

    public PreguntaEntity actualizar(Long id, PreguntaEntity datos) {
        PreguntaEntity existente = obtenerPorId(id);

        existente.setFkDimensionCuestionario(datos.getFkDimensionCuestionario());
        existente.setFkEscala(datos.getFkEscala());
        existente.setNumero(datos.getNumero());
        existente.setDescripcion(datos.getDescripcion());

        return repository.save(existente);
    }

    public PreguntaEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pregunta no encontrada con id: " + id));
    }

    public List<PreguntaEntity> listarTodos() {
        return repository.findAll();
    }

    public List<PreguntaEntity> listarPorDimensionCuestionario(Long fkDimensionCuestionario) {
        return repository.findByFkDimensionCuestionario(fkDimensionCuestionario);
    }

    public List<PreguntaEntity> listarPorEscala(Long fkEscala) {
        return repository.findByFkEscala(fkEscala);
    }

}
