package com.riesgopsicosocial.infrastructure.adapter.in.rest.dominiocuestionario;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.dominiocuestionario.dto.DominioCuestionarioRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.dominiocuestionario.dto.DominioCuestionarioResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dominio-cuestionarios")
public class DominioCuestionarioController {

    private final DominioCuestionarioService service;

    public DominioCuestionarioController(DominioCuestionarioService service) {
        this.service = service;
    }

    private DominioCuestionarioResponse toResponse(DominioCuestionarioEntity entidad) {
        return new DominioCuestionarioResponse(
                entidad.getId(),
                entidad.getFkDominio(),
                entidad.getFkCuestionario(),
                entidad.getFactorTransformacion());
    }

    private DominioCuestionarioEntity toEntity(DominioCuestionarioRequest request) {
        DominioCuestionarioEntity entidad = new DominioCuestionarioEntity();
        entidad.setFkDominio(request.fkDominio());
        entidad.setFkCuestionario(request.fkCuestionario());
        entidad.setFactorTransformacion(request.factorTransformacion());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DominioCuestionarioResponse>> crear(
            @Valid @RequestBody DominioCuestionarioRequest request) {
        DominioCuestionarioEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(
                        "Configuración dominio-cuestionario creada exitosamente",
                        toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DominioCuestionarioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DominioCuestionarioRequest request) {
        DominioCuestionarioEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Configuración dominio-cuestionario actualizada exitosamente",
                        toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DominioCuestionarioResponse>>> listarTodos(
            @RequestParam(required = false) Long fkDominio,
            @RequestParam(required = false) Long fkCuestionario) {

        List<DominioCuestionarioEntity> resultado;

        if (fkDominio != null) {
            resultado = service.listarPorDominio(fkDominio);
        } else if (fkCuestionario != null) {
            resultado = service.listarPorCuestionario(fkCuestionario);
        } else {
            resultado = service.listarTodos();
        }

        List<DominioCuestionarioResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.success("Listado de configuraciones dominio-cuestionario", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DominioCuestionarioResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Configuración dominio-cuestionario encontrada",
                        toResponse(service.obtenerPorId(id))));
    }

}
