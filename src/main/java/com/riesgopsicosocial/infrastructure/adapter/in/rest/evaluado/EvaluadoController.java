package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto.EvaluadoRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto.EvaluadoResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoService;
import com.riesgopsicosocial.infrastructure.security.AlcanceUsuario;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/evaluados")
public class EvaluadoController {

    private final EvaluadoService service;
    private final AlcanceUsuario alcance;

    public EvaluadoController(EvaluadoService service, AlcanceUsuario alcance) {
        this.service = service;
        this.alcance = alcance;
    }

    private EvaluadoResponse toResponse(EvaluadoEntity evaluadoEntity) {
        return new EvaluadoResponse(
                evaluadoEntity.getId(),
                evaluadoEntity.getNumeroIdentificacion(),
                evaluadoEntity.getNombre(),
                evaluadoEntity.getApellido(),
                evaluadoEntity.getFkSexo(),
                evaluadoEntity.getAnioNacimiento()
        );
    }

    private EvaluadoEntity toEntity(EvaluadoRequest evaluadoRequest) {
        EvaluadoEntity evaluadoEntity = new EvaluadoEntity();
        evaluadoEntity.setNumeroIdentificacion(evaluadoRequest.numeroIdentificacion());
        evaluadoEntity.setNombre(evaluadoRequest.nombre());
        evaluadoEntity.setApellido(evaluadoRequest.apellido());
        evaluadoEntity.setFkSexo(evaluadoRequest.fkSexo());
        evaluadoEntity.setAnioNacimiento(evaluadoRequest.anioNacimiento());
        return evaluadoEntity;
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadoResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody EvaluadoRequest request) {
        EvaluadoEntity actualizado = service.actualizar(id, toEntity(request));

        return ResponseEntity.ok(
                ResponseBuilder
                        .success(
                                "Evaluado actualizado exitosamente",
                                toResponse(actualizado))
        );
    }

    /**
     * Por documento exacto se encuentra aunque sea de otra empresa (decisión del usuario: para poder
     * vincularlo a su cliente). El listado general solo trae los de sus clientes.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluadoResponse>>> listarTodos(
            @RequestParam(required = false) String numeroIdentificacion) {

        if (numeroIdentificacion != null && !numeroIdentificacion.isBlank()) {
            EvaluadoResponse encontrado = toResponse(
                    service.obtenerPorNumeroIdentificacion(numeroIdentificacion));
            return ResponseEntity.ok(ResponseBuilder.success("Evaluado encontrado", List.of(encontrado)));
        }

        Optional<Set<Long>> visibles = alcance.evaluadosVisibles();
        List<EvaluadoResponse> data = service.listarTodos().stream()
                .filter(e -> visibles.map(v -> v.contains(e.getId())).orElse(true))
                .map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ResponseBuilder.success("Listado de evaluados", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadoResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder
                        .success(
                                "Evaluado encontrado",
                                toResponse(service.obtenerPorId(id)))
        );
    }

}
