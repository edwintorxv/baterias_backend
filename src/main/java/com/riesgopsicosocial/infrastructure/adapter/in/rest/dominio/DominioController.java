package com.riesgopsicosocial.infrastructure.adapter.in.rest.dominio;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioService;
import com.riesgopsicosocial.shared.catalogo.CatalogoCrudController;
import com.riesgopsicosocial.shared.catalogo.CatalogoRequest;
import com.riesgopsicosocial.shared.catalogo.CatalogoResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dominios")
public class DominioController {

    private final DominioService service;

    public DominioController(DominioService service) {
        this.service = service;
    }

    private CatalogoResponse toResponse(DominioEntity e) {
        return new CatalogoResponse(e.getId(), e.getNombre());
    }

    private DominioEntity toEntity(CatalogoRequest request) {
        DominioEntity entidad = new DominioEntity();
        entidad.setNombre(request.nombre());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CatalogoResponse>> crear(@Valid @RequestBody CatalogoRequest request) {
        DominioEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseBuilder.success(
                        "Dominio creado exitosamente", toResponse(creado))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> actualizar(@PathVariable Long id,
                                                                    @Valid @RequestBody CatalogoRequest request) {
        DominioEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Dominio actualizado exitosamente", toResponse(actualizado))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Dominio encontrado", toResponse(service.obtenerPorId(id)))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(
                ResponseBuilder.success("Listado de dominios", data));
    }

}
