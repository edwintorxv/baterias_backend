package com.riesgopsicosocial.infrastructure.adapter.in.rest.tipocargo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipovivienda.TipoViviendaService;
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
@RequestMapping("/tipo-cargo")
public class TipoCargoController {

    private final TipoCargoService service;

    public TipoCargoController(TipoCargoService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(TipoCargoEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Tipo contrato", data));
    }

}
