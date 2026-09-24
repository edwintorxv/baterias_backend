package com.riesgopsicosocial.infrastructure.adapter.in.rest.respuesta;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.respuesta.dto.RespuestaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.respuesta.dto.RespuestaResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/respuestas")
public class RespuestaController {

    private final RespuestaService service;

    public RespuestaController(RespuestaService service) {
        this.service = service;
    }

    private RespuestaResponse toResponse(RespuestaEntity entidad) {
        return new RespuestaResponse(
                entidad.getId(),
                entidad.getFkAplicacion(),
                entidad.getFkPregunta(),
                entidad.getFkOpcionRespuesta(),
                entidad.getValorObtenido(),
                entidad.getFechaRespuesta());
    }

    private RespuestaEntity toEntity(RespuestaRequest request) {
        RespuestaEntity entidad = new RespuestaEntity();
        entidad.setFkAplicacion(request.fkAplicacion());
        entidad.setFkPregunta(request.fkPregunta());
        entidad.setFkOpcionRespuesta(request.fkOpcionRespuesta());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RespuestaResponse>> crear(@Valid @RequestBody RespuestaRequest request) {
        RespuestaEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Respuesta registrada exitosamente", toResponse(creado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RespuestaResponse>>> listarPorAplicacion(
            @RequestParam Long fkAplicacion) {

        List<RespuestaResponse> data = service.listarPorAplicacion(fkAplicacion).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de respuestas de la aplicación", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RespuestaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Respuesta encontrada", toResponse(service.obtenerPorId(id))));
    }

}
