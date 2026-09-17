package com.riesgopsicosocial.infrastructure.adapter.in.rest.sectoreconomico;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sectoreconimico.SectorEconomicoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sectoreconimico.SectorEconomicoService;
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
@RequestMapping("/sectores-economicos")
public class SectorEconimicoController {

    private final SectorEconomicoService service;

    public SectorEconimicoController(SectorEconomicoService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(SectorEconomicoEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de sectores economicos", data));
    }

}
