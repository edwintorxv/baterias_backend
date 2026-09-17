package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluadoService {

    private final EvaluadoJpaRepository repository;

    public EvaluadoService(EvaluadoJpaRepository repository) {
        this.repository = repository;
    }

    public EvaluadoEntity crear(EvaluadoEntity entidad) {
        if (repository.existsByNumeroIdentificacion(entidad.getNumeroIdentificacion())) {
            throw new BusinessException(
                    "Ya existe un evaluado con número de identificación: " + entidad.getNumeroIdentificacion());
        }
        return repository.save(entidad);
    }

    public EvaluadoEntity actualizar(Long id, EvaluadoEntity datos) {
        EvaluadoEntity existente = obtenerPorId(id);

        if (!existente.getNumeroIdentificacion().equalsIgnoreCase(datos.getNumeroIdentificacion())
                && repository.existsByNumeroIdentificacion(datos.getNumeroIdentificacion())) {
            throw new BusinessException(
                    "Ya existe un evaluado con número de identificación: " + datos.getNumeroIdentificacion());
        }

        existente.setNumeroIdentificacion(datos.getNumeroIdentificacion());
        existente.setNombre(datos.getNombre());
        existente.setApellido(datos.getApellido());
        existente.setFkSexo(datos.getFkSexo());
        existente.setAnioNacimiento(datos.getAnioNacimiento());
        existente.setFkEstadoCivil(datos.getFkEstadoCivil());
        existente.setFkNivelAcademico(datos.getFkNivelAcademico());
        existente.setFkCiudadResidencia(datos.getFkCiudadResidencia());
        existente.setFkEstratoSocioeconomico(datos.getFkEstratoSocioeconomico());
        existente.setFkTipoVivienda(datos.getFkTipoVivienda());
        existente.setOcupacionProfesion(datos.getOcupacionProfesion());
        existente.setFamiliaresDependientesEconomicamente(datos.getFamiliaresDependientesEconomicamente());

        return repository.save(existente);
    }

    public EvaluadoEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluado no encontrado con id: " + id));
    }

    public List<EvaluadoEntity> listarTodos() {
        return repository.findAll();
    }

    public EvaluadoEntity obtenerPorNumeroIdentificacion(String numeroIdentificacion) {
        return repository.findByNumeroIdentificacion(numeroIdentificacion)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Evaluado no encontrado con número de identificación: " + numeroIdentificacion));
    }

}
