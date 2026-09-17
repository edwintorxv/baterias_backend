package com.riesgopsicosocial.shared.catalogo;

import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class CatalogoCrudController<T extends Identificable & Nombrable> {

    private final CatalogoCrudService<T> service;
    private final Supplier<T> factory;
    private final String nombreEntidad;

    protected CatalogoCrudController(CatalogoCrudService<T> service, Supplier<T> factory, String nombreEntidad) {
        this.service = service;
        this.factory = factory;
        this.nombreEntidad = nombreEntidad;
    }

    private T toEntity(CatalogoRequest request) {
        T entidad = factory.get();
        entidad.setNombre(request.nombre());
        return entidad;
    }

    private CatalogoResponse toResponse(T entidad) {
        return new CatalogoResponse(entidad.getId(), entidad.getNombre());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CatalogoResponse>> crear(@Valid @RequestBody CatalogoRequest request) {
        T creado = service.crear(toEntity(request));
        ApiResponse<CatalogoResponse> body = ResponseBuilder.success(
                nombreEntidad + " creado exitosamente", toResponse(creado)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> actualizar(@PathVariable Long id,
                                                                    @Valid @RequestBody CatalogoRequest request) {
        T actualizado = service.actualizar(id, toEntity(request));

        ApiResponse<CatalogoResponse> body = ResponseBuilder.success(
                nombreEntidad + " actualizado exitosamente", toResponse(actualizado)
        );

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoResponse>> obtenerPorId(@PathVariable Long id) {
        ApiResponse<CatalogoResponse> body = ResponseBuilder.success(
                nombreEntidad + " encontrado", toResponse(service.obtenerPorId(id))
        );

        return ResponseEntity.ok(body);

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> listarTodos() {
        List<CatalogoResponse> data = service.listarTodos().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        ApiResponse<List<CatalogoResponse>> body = ResponseBuilder.success(
                "Listado de " + nombreEntidad, data
        );

        return ResponseEntity.ok(body);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminiar(@PathVariable long id) {
        service.eliminar(id);

        ApiResponse<Void> body = ResponseBuilder.success(nombreEntidad + " eliminado corectamente", null);

        return ResponseEntity.ok(body);
    }

}
