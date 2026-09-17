package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio.dto.BaremoDominioRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio.dto.BaremoDominioResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/baremo-dominios")
public class BaremoDominioController {

    private final BaremoDominioService service;

    public BaremoDominioController(BaremoDominioService service) {
        this.service = service;
    }

    private BaremoDominioResponse toResponse(BaremoDominioEntity entidad) {
        return new BaremoDominioResponse(
                entidad.getId(),
                entidad.getFkDominioCuestionario(),
                entidad.getFkNivelRiesgo(),
                entidad.getValorMinimo(),
                entidad.getValorMaximo());
    }

    private BaremoDominioEntity toEntity(BaremoDominioRequest request) {
        BaremoDominioEntity entidad = new BaremoDominioEntity();
        entidad.setFkDominioCuestionario(request.fkDominioCuestionario());
        entidad.setFkNivelRiesgo(request.fkNivelRiesgo());
        entidad.setValorMinimo(request.valorMinimo());
        entidad.setValorMaximo(request.valorMaximo());
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BaremoDominioResponse>> crear(
            @Valid @RequestBody BaremoDominioRequest request) {
        BaremoDominioEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Baremo de dominio creado exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BaremoDominioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BaremoDominioRequest request) {
        BaremoDominioEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success("Baremo de dominio actualizado exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BaremoDominioResponse>>> listarTodos(
            @RequestParam(required = false) Long fkDominioCuestionario,
            @RequestParam(required = false) Long fkNivelRiesgo) {

        List<BaremoDominioEntity> resultado;

        if (fkDominioCuestionario != null) {
            resultado = service.listarPorDominioCuestionario(fkDominioCuestionario);
        } else if (fkNivelRiesgo != null) {
            resultado = service.listarPorNivelRiesgo(fkNivelRiesgo);
        } else {
            resultado = service.listarTodos();
        }

        List<BaremoDominioResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de baremos de dominio", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BaremoDominioResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success("Baremo de dominio encontrado", toResponse(service.obtenerPorId(id))));
    }

}
