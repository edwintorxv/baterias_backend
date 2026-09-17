package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.AplicacionRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.AplicacionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/aplicaciones")
public class AplicacionController {

    private final AplicacionService service;

    public AplicacionController(AplicacionService service) {
        this.service = service;
    }

    private AplicacionResponse toResponse(AplicacionEntity aplicacionEntity) {
        return new AplicacionResponse(
                aplicacionEntity.getId(),
                aplicacionEntity.getFkEvaluadoCliente(),
                aplicacionEntity.getFechaAplicacion(),
                aplicacionEntity.getObservaciones(),
                aplicacionEntity.getEstado()
        );
    }

    private AplicacionEntity toEntity(AplicacionRequest aplicacionRequest) {
        AplicacionEntity aplicacionEntity = new AplicacionEntity();
        aplicacionEntity.setFkEvaluadoCliente(aplicacionRequest.fkEvaluadoCliente());
        aplicacionEntity.setFechaAplicacion(aplicacionRequest.fechaAplicacion());
        aplicacionEntity.setObservaciones(aplicacionRequest.observaciones());
        aplicacionEntity.setEstado(aplicacionRequest.estado());
        return aplicacionEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AplicacionResponse>> crear(@Valid @RequestBody AplicacionRequest request) {
        AplicacionEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(
                        "Aplicación creada exitosamente",
                        toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AplicacionResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody AplicacionRequest request) {
        AplicacionEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Aplicación actualizada exitosamente",
                        toResponse(actualizado))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AplicacionResponse>>> listarTodos(
            @RequestParam(required = false) Long fkEvaluadoCliente,
            @RequestParam(required = false) String estado) {

        List<AplicacionEntity> resultado;
        if (fkEvaluadoCliente != null) {
            resultado = service.listarPorEvaluadoCliente(fkEvaluadoCliente);
        } else if (estado != null && !estado.isBlank()) {
            resultado = service.listarPorEstado(estado);
        } else {
            resultado = service.listarTodos();
        }

        List<AplicacionResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors
                        .toList());
        return ResponseEntity.ok(ResponseBuilder.success("Listado de aplicaciones", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AplicacionResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Aplicación encontrada",
                        toResponse(service.obtenerPorId(id)))
        );
    }

}
