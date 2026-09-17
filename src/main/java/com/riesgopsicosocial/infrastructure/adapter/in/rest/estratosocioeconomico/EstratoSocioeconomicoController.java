package com.riesgopsicosocial.infrastructure.adapter.in.rest.estratosocioeconomico;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.estratosocioeconomico.EstratoSocioeconomicoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.estratosocioeconomico.EstratoSocioeconomicoService;
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
@RequestMapping("/estratos-socioeconomicos")
public class EstratoSocioeconomicoController {

    private final EstratoSocioeconomicoService service;

    public EstratoSocioeconomicoController(EstratoSocioeconomicoService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(EstratoSocioeconomicoEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Estrato socioeconomico", data));
    }


}
