package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RespuestaService {

    private final RespuestaJpaRepository repository;
    private final PreguntaJpaRepository preguntaRepository;
    private final EscalaDetalleJpaRepository escalaDetalleRepository;

    public RespuestaService(RespuestaJpaRepository repository,
                             PreguntaJpaRepository preguntaRepository,
                             EscalaDetalleJpaRepository escalaDetalleRepository) {
        this.repository = repository;
        this.preguntaRepository = preguntaRepository;
        this.escalaDetalleRepository = escalaDetalleRepository;
    }

    public RespuestaEntity crear(RespuestaEntity entidad) {
        PreguntaEntity pregunta = preguntaRepository.findById(entidad.getFkPregunta())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pregunta no encontrada con id: " + entidad.getFkPregunta()));

        EscalaDetalleEntity escalaDetalle = escalaDetalleRepository
                .findByFkEscalaAndFkOpcionRespuesta(pregunta.getFkEscala(), entidad.getFkOpcionRespuesta())
                .orElseThrow(() -> new BusinessException(
                        "La opción de respuesta seleccionada no tiene un valor definido para la escala de esta pregunta"));

        entidad.setValorObtenido(escalaDetalle.getValor());
        return repository.save(entidad);
    }

    public RespuestaEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Respuesta no encontrada con id: " + id));
    }

    public List<RespuestaEntity> listarPorAplicacion(Long fkAplicacion) {
        return repository.findByFkAplicacion(fkAplicacion);
    }

}
