package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimension;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.dimension.dto.DimensionRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.dimension.dto.DimensionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dimensiones")
public class DimensionController {

    private final DimensionService service;

    public DimensionController(DimensionService service) {
        this.service = service;
    }

    private DimensionResponse toResponse(DimensionEntity entity) {
        return new DimensionResponse((entity.getId()), entity.getFkDominio(), entity.getNombre());
    }

    private DimensionEntity toEntity(DimensionRequest request) {
        DimensionEntity entidad = new DimensionEntity();
        entidad.setFkDominio(request.fkDominio());
        entidad.setNombre(request.descripcion());
        return entidad;

    }

    @PostMapping
    public ResponseEntity<ApiResponse<DimensionResponse>> crear(@Valid @RequestBody DimensionRequest request) {
        DimensionEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(
                        "Dimension creada exitosamente",
                        toResponse(creado)
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DimensionResponse>> actualizar(@PathVariable Long id,
                                                                     @Valid @RequestBody DimensionRequest request) {
        DimensionEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Dimension actualizada exitosamente",
                        toResponse(actualizado)
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DimensionResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Dimension encontrada con exito",
                        toResponse(service.obtenerPorId(id))
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DimensionResponse>>> listarTodos() {
        List<DimensionResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success(
                "Listado de dimensiones", data
        ));
    }

}
