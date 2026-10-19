package com.riesgopsicosocial.infrastructure.adapter.in.rest.industria;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria.IndustriaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria.IndustriaService;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.industria.dto.IndustriaResponse;
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

    private IndustriaResponse toResponse(IndustriaEntity entity) {
        return new IndustriaResponse(entity.getId(), entity.getFkSectorEconomico(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IndustriaResponse>>> listar(
            @RequestParam(required = false) Long sectorEconomicoId) {

        List<IndustriaEntity> entidades = (sectorEconomicoId != null)
                ? service.listarPorSectorEconomico(sectorEconomicoId)
                : service.listarTodos();

        List<IndustriaResponse> data = entidades.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Industria", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndustriaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Industria encontrada", toResponse(service.obtenerPorId(id)))
        );
    }

}
