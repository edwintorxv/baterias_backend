package com.riesgopsicosocial.infrastructure.adapter.in.rest.tiposalario;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiposalario.TipoSalarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiposalario.TipoSalarioService;
import com.riesgopsicosocial.shared.catalogo.CatalogoCrudController;
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
@RequestMapping("/tipo-salario")
public class TipoSalarioController {

    private final TipoSalarioService service;

    public TipoSalarioController(TipoSalarioService service) {
        this.service = service;
    }

    public CatalogoResponse toResponse(TipoSalarioEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Tipo salario", data));
    }

}
