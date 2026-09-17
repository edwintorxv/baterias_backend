package com.riesgopsicosocial.infrastructure.adapter.in.rest.escala;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaService;
import com.riesgopsicosocial.shared.catalogo.CatalogoResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/escala")
public class EscalaController {

    private final EscalaService service;

    public EscalaController(EscalaService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(EscalaEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Escala", data));
    }

}
