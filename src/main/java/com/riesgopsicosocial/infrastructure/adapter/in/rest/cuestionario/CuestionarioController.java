package com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario;


import com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario.dto.CuestionarioRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario.dto.CuestionarioResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cuestionarios")
public class CuestionarioController {

    private final CuestionarioService service;

    public CuestionarioController(CuestionarioService service) {
        this.service = service;
    }

    private CuestionarioResponse toResponse(CuestionarioEntity cuestionarioEntity) {
        return new CuestionarioResponse(
                cuestionarioEntity.getId(),
                cuestionarioEntity.getNombre(),
                cuestionarioEntity.getForma()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CuestionarioResponse>> crear(@Valid @RequestBody CuestionarioRequest request) {
        CuestionarioEntity entidad = new CuestionarioEntity();
        entidad.setForma(request.forma());
        entidad.setNombre(request.descripcion());
        CuestionarioEntity creado = service.crear(entidad);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ResponseBuilder.success(
                                "Formulario creado existosamente",
                                toResponse(creado))
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CuestionarioResponse>>> listarTodos() {
        List<CuestionarioResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de formularios", data));

    }

}
