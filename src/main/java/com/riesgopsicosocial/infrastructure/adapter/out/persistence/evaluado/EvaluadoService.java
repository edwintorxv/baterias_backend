package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.VinculacionPersonaService;
import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EvaluadoService {

    private final EvaluadoJpaRepository repository;

    public EvaluadoService(EvaluadoJpaRepository repository) {
        this.repository = repository;
    }

    /** Corrige los datos que no cambian. Crear: solo por VinculacionPersonaService (siempre con un cliente). */
    public EvaluadoEntity actualizar(Long id, EvaluadoEntity datos) {
        EvaluadoEntity existente = obtenerPorId(id);
        // Misma regla de edad que al vincular (si viene el año).
        if (datos.getAnioNacimiento() != null) {
            VinculacionPersonaService.errorAnioNacimiento(datos.getAnioNacimiento()).ifPresent(error -> {
                throw new BusinessException(error);
            });
        }

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

        existente.setFechaModificado(LocalDateTime.now());
        existente.setUsuarioModifica(UsuarioActual.nombre());
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
