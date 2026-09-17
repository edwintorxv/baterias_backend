package com.riesgopsicosocial.infrastructure.adapter.in.rest.sexo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoService;
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
@RequestMapping("/sexos")
public class SexoController {

    private final SexoService service;

    public SexoController(SexoService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(SexoEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Sexo", data));
    }

}
