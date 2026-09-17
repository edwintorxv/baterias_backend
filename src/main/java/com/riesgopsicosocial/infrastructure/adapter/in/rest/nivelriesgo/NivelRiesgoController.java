package com.riesgopsicosocial.infrastructure.adapter.in.rest.nivelriesgo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoService;
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
@RequestMapping("/niveles-riesgos")
public class NivelRiesgoController {

    private final NivelRiesgoService service;

    public NivelRiesgoController(NivelRiesgoService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(NivelRiesgoEntity entity){
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Nivel de riesgo", data));
    }

}