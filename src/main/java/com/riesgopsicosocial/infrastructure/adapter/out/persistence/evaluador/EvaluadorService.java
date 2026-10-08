package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EvaluadorService {

    private static final Set<String> TIPOS_FIRMA = Set.of("image/png", "image/jpeg");
    private static final long TAMANO_MAXIMO_FIRMA = 1024 * 1024;

    private final EvaluadorJpaRepository repository;

    public EvaluadorService(EvaluadorJpaRepository repository) {
        this.repository = repository;
    }

    public EvaluadorEntity crear(EvaluadorEntity entidad) {
        if (repository.existsByNumeroIdentificacion(entidad.getNumeroIdentificacion())) {
            throw new BusinessException(
                    "Ya existe un evaluador con número de identificación: " + entidad.getNumeroIdentificacion());
        }
        if (entidad.getActivo() == null) {
            entidad.setActivo(true);
        }
        return repository.save(entidad);
    }

    /** La firma no se toca aquí: tiene su propio endpoint. */
    public EvaluadorEntity actualizar(Long id, EvaluadorEntity datos) {
        EvaluadorEntity existente = obtenerPorId(id);

        if (!existente.getNumeroIdentificacion().equalsIgnoreCase(datos.getNumeroIdentificacion())
                && repository.existsByNumeroIdentificacion(datos.getNumeroIdentificacion())) {
            throw new BusinessException(
                    "Ya existe un evaluador con número de identificación: " + datos.getNumeroIdentificacion());
        }

        existente.setNumeroIdentificacion(datos.getNumeroIdentificacion());
        existente.setNombre(datos.getNombre());
        existente.setProfesion(datos.getProfesion());
        existente.setPosgrado(datos.getPosgrado());
        existente.setTarjetaProfesional(datos.getTarjetaProfesional());
        existente.setLicenciaSaludOcupacional(datos.getLicenciaSaludOcupacional());
        existente.setFechaExpedicionLicencia(datos.getFechaExpedicionLicencia());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        return repository.save(existente);
    }

    public EvaluadorEntity guardarFirma(Long id, byte[] imagen, String tipoContenido) {
        EvaluadorEntity existente = obtenerPorId(id);
        if (imagen == null || imagen.length == 0) {
            throw new ValidationException("La firma está vacía", List.of("Envíe una imagen PNG o JPG en el campo 'archivo'"));
        }
        if (tipoContenido == null || !TIPOS_FIRMA.contains(tipoContenido)) {
            throw new ValidationException("Tipo de archivo no permitido para la firma: " + tipoContenido,
                    List.of("Tipos permitidos: " + String.join(", ", TIPOS_FIRMA)));
        }
        if (imagen.length > TAMANO_MAXIMO_FIRMA) {
            throw new ValidationException("La firma supera el tamaño máximo de 1 MB");
        }
        existente.setFirma(imagen);
        existente.setFirmaTipoContenido(tipoContenido);
        return repository.save(existente);
    }

    public EvaluadorEntity obtenerConFirma(Long id) {
        EvaluadorEntity existente = obtenerPorId(id);
        if (existente.getFirma() == null) {
            throw new ResourceNotFoundException("El evaluador " + id + " no tiene firma registrada");
        }
        return existente;
    }

    public EvaluadorEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluador no encontrado con id: " + id));
    }

    public List<EvaluadorEntity> listarTodos() {
        return repository.findAll();
    }

    public List<EvaluadorEntity> listarPorActivo(Boolean activo) {
        return repository.findByActivo(activo);
    }

    public EvaluadorEntity obtenerPorNumeroIdentificacion(String numeroIdentificacion) {
        return repository.findByNumeroIdentificacion(numeroIdentificacion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluador no encontrado con número de identificación: " + numeroIdentificacion));
    }

}
