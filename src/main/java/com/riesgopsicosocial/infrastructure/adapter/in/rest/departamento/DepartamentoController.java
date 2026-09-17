package com.riesgopsicosocial.infrastructure.adapter.in.rest.departamento;


import com.riesgopsicosocial.infrastructure.adapter.in.rest.departamento.dto.DepartamentoRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.departamento.dto.DepartamentoResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento.DepartamentoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento.DepartamentoService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/departamentos")
public class DepartamentoController {

    private final DepartamentoService service;

    public DepartamentoController(DepartamentoService service) {
        this.service = service;
    }

    private DepartamentoResponse toResponse(DepartamentoEntity departamentoEntity) {
        return new DepartamentoResponse(
                departamentoEntity.getId(),
                departamentoEntity.getCodigoDane(),
                departamentoEntity.getNombre()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartamentoResponse>>> listarTodos() {
        List<DepartamentoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de departamentos", data));
    }

}
