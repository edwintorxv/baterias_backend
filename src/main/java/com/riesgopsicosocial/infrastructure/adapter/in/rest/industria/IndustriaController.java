package com.riesgopsicosocial.infrastructure.adapter.in.rest.industria;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria.IndustriaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria.IndustriaService;
import com.riesgopsicosocial.shared.catalogo.CatalogoResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/industrias")
public class IndustriaController {

    private final IndustriaService service;

    public IndustriaController(IndustriaService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(IndustriaEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listar(
            @RequestParam(required = false) Long sectorEconomicoId) {

        List<IndustriaEntity> entidades = (sectorEconomicoId != null)
                ? service.listarPorSectorEconomico(sectorEconomicoId)
                : service.listarTodos();

        List<CatalogoResponse> data = entidades.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Industria", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Industria encontrada", toResponse(service.obtenerPorId(id)))
        );
    }

}
