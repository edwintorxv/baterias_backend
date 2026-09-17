package com.riesgopsicosocial.infrastructure.adapter.in.rest.opcionrespuesta;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaService;
import com.riesgopsicosocial.shared.catalogo.CatalogoCrudController;
import com.riesgopsicosocial.shared.catalogo.CatalogoResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("opcion-respueta")
public class OpcionRespuestaController {

    private final OpcionRespuestaService service;

    public OpcionRespuestaController(OpcionRespuestaService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(OpcionRespuestaEntity entity) {
        return new CatalogoResponse(entity.getId(), entity.getNombre());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de Opcion respuesta", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Opcion respuesta econtrada",
                        toResponse(service.obtenerPorId(id))
                )
        );
    }

}
