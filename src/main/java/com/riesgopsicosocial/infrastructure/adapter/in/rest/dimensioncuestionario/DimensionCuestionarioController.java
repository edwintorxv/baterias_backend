package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto.DimensionCuestionarioRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto.DimensionCuestionarioResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dimension-cuestionarios")
public class DimensionCuestionarioController {

    private final DimensionCuestionarioService service;

    public DimensionCuestionarioController(DimensionCuestionarioService service) {
        this.service = service;
    }

    private DimensionCuestionarioResponse toResponse(DimensionCuestionarioEntity entidad) {
        return new DimensionCuestionarioResponse(
                entidad.getId(),
                entidad.getFkDimension(),
                entidad.getFkCuestionario(),
                entidad.getFactorTransformacion());
    }

    private DimensionCuestionarioEntity toEntity(DimensionCuestionarioRequest request) {
        DimensionCuestionarioEntity entidad = new DimensionCuestionarioEntity();
        entidad.setFkDimension(request.fkDimension());
        entidad.setFkCuestionario(request.fkCuestionario());
        entidad.setFactorTransformacion(request.factorTransformacion());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DimensionCuestionarioResponse>> crear(
            @Valid @RequestBody DimensionCuestionarioRequest request) {
        DimensionCuestionarioEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(
                        "Configuración dimensión-cuestionario creada exitosamente",
                        toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DimensionCuestionarioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DimensionCuestionarioRequest request) {
        DimensionCuestionarioEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Configuración dimensión-cuestionario actualizada exitosamente",
                        toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DimensionCuestionarioResponse>>> listarTodos(
            @RequestParam(required = false) Long fkDimension,
            @RequestParam(required = false) Long fkCuestionario) {

        List<DimensionCuestionarioEntity> resultado;

        if (fkDimension != null) {
            resultado = service.listarPorDimension(fkDimension);
        } else if (fkCuestionario != null) {
            resultado = service.listarPorCuestionario(fkCuestionario);
        } else {
            resultado = service.listarTodos();
        }

        List<DimensionCuestionarioResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.success("Listado de configuraciones dimensión-cuestionario", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DimensionCuestionarioResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Configuración dimensión-cuestionario encontrada",
                        toResponse(service.obtenerPorId(id))));
    }

}
