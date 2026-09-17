package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension.dto.BaremoDimensionRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension.dto.BaremoDimensionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/baremo-dimensiones")
public class BaremoDimensionController {

    private final BaremoDimensionService service;

    public BaremoDimensionController(BaremoDimensionService service) {
        this.service = service;
    }

    private BaremoDimensionResponse toResponse(BaremoDimensionEntity entidad) {
        return new BaremoDimensionResponse(
                entidad.getId(),
                entidad.getFkDimensionCuestionario(),
                entidad.getFkNivelRiesgo(),
                entidad.getValorMinimo(),
                entidad.getValorMaximo());
    }

    private BaremoDimensionEntity toEntity(BaremoDimensionRequest request) {
        BaremoDimensionEntity entidad = new BaremoDimensionEntity();
        entidad.setFkDimensionCuestionario(request.fkDimensionCuestionario());
        entidad.setFkNivelRiesgo(request.fkNivelRiesgo());
        entidad.setValorMinimo(request.valorMinimo());
        entidad.setValorMaximo(request.valorMaximo());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BaremoDimensionResponse>> crear(
            @Valid @RequestBody BaremoDimensionRequest request) {
        BaremoDimensionEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Baremo de dimensión creado exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BaremoDimensionResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BaremoDimensionRequest request) {
        BaremoDimensionEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success("Baremo de dimensión actualizado exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BaremoDimensionResponse>>> listarTodos(
            @RequestParam(required = false) Long fkDimensionCuestionario,
            @RequestParam(required = false) Long fkNivelRiesgo) {

        List<BaremoDimensionEntity> resultado;

        if (fkDimensionCuestionario != null) {
            resultado = service.listarPorDimensionCuestionario(fkDimensionCuestionario);
        } else if (fkNivelRiesgo != null) {
            resultado = service.listarPorNivelRiesgo(fkNivelRiesgo);
        } else {
            resultado = service.listarTodos();
        }

        List<BaremoDimensionResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de baremos de dimensión", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BaremoDimensionResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Baremo de dimensión encontrado", toResponse(service.obtenerPorId(id))));
    }

}
