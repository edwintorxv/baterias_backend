package com.riesgopsicosocial.infrastructure.adapter.in.rest.pregunta;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.pregunta.dto.PreguntaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.pregunta.dto.PreguntaResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/preguntas")
public class PreguntaController {

    private final PreguntaService service;

    public PreguntaController(PreguntaService service) {
        this.service = service;
    }

    private PreguntaResponse toResponse(PreguntaEntity entidad) {
        return new PreguntaResponse(
                entidad.getId(),
                entidad.getFkDimensionCuestionario(),
                entidad.getFkEscala(),
                entidad.getNumero(),
                entidad.getDescripcion());
    }

    private PreguntaEntity toEntity(PreguntaRequest request) {
        PreguntaEntity entidad = new PreguntaEntity();
        entidad.setFkDimensionCuestionario(request.fkDimensionCuestionario());
        entidad.setFkEscala(request.fkEscala());
        entidad.setNumero(request.numero());
        entidad.setDescripcion(request.descripcion());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PreguntaResponse>> crear(
            @Valid @RequestBody PreguntaRequest request) {
        PreguntaEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Pregunta creada exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PreguntaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PreguntaRequest request) {
        PreguntaEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success("Pregunta actualizada exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PreguntaResponse>>> listarTodos(
            @RequestParam(required = false) Long fkDimensionCuestionario,
            @RequestParam(required = false) Long fkEscala) {

        List<PreguntaEntity> resultado;

        if (fkDimensionCuestionario != null) {
            resultado = service.listarPorDimensionCuestionario(fkDimensionCuestionario);
        } else if (fkEscala != null) {
            resultado = service.listarPorEscala(fkEscala);
        } else {
            resultado = service.listarTodos();
        }

        List<PreguntaResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de preguntas", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PreguntaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Pregunta encontrada", toResponse(service.obtenerPorId(id))));
    }

}
