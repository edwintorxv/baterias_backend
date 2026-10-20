package com.riesgopsicosocial.infrastructure.adapter.in.rest.tipocargo;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.tipocargo.dto.TipoCargoResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoService;
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

    private TipoCargoResponse toResponse(TipoCargoEntity entity) {
        return new TipoCargoResponse(entity.getId(), entity.getFkGrupoOcupacional(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoCargoResponse>>> listarTodos() {
        List<TipoCargoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Tipo cargo", data));
    }

}
