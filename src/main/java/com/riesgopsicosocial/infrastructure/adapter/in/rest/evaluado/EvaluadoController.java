package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto.EvaluadoRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto.EvaluadoResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/evaluados")
public class EvaluadoController {

    private final EvaluadoService service;

    public EvaluadoController(EvaluadoService service) {
        this.service = service;
    }

    private EvaluadoResponse toResponse(EvaluadoEntity evaluadoEntity) {
        return new EvaluadoResponse(
                evaluadoEntity.getId(),
                evaluadoEntity.getNumeroIdentificacion(),
                evaluadoEntity.getNombre(),
                evaluadoEntity.getApellido(),
                evaluadoEntity.getFkSexo(),
                evaluadoEntity.getAnioNacimiento(),
                evaluadoEntity.getFkEstadoCivil(),
                evaluadoEntity.getFkNivelAcademico(),
                evaluadoEntity.getFkCiudadResidencia(),
                evaluadoEntity.getFkEstratoSocioeconomico(),
                evaluadoEntity.getFkTipoVivienda(),
                evaluadoEntity.getOcupacionProfesion(),
                evaluadoEntity.getFamiliaresDependientesEconomicamente()
        );
    }

    private EvaluadoEntity toEntity(EvaluadoRequest evaluadoRequest) {
        EvaluadoEntity evaluadoEntity = new EvaluadoEntity();
        evaluadoEntity.setNumeroIdentificacion(evaluadoRequest.numeroIdentificacion());
        evaluadoEntity.setNombre(evaluadoRequest.nombre());
        evaluadoEntity.setApellido(evaluadoRequest.apellido());
        evaluadoEntity.setFkSexo(evaluadoRequest.fkSexo());
        evaluadoEntity.setAnioNacimiento(evaluadoRequest.anioNacimiento());
        evaluadoEntity.setFkEstadoCivil(evaluadoRequest.fkEstadoCivil());
        evaluadoEntity.setFkNivelAcademico(evaluadoRequest.fkNivelAcademico());
        evaluadoEntity.setFkCiudadResidencia(evaluadoRequest.fkCiudadResidencia());
        evaluadoEntity.setFkEstratoSocioeconomico(evaluadoRequest.fkEstratoSocioeconomico());
        evaluadoEntity.setFkTipoVivienda(evaluadoRequest.fkTipoVivienda());
        evaluadoEntity.setOcupacionProfesion(evaluadoRequest.ocupacionProfesion());
        evaluadoEntity.setFamiliaresDependientesEconomicamente(evaluadoRequest.familiaresDependientesEconomicamente());
        return evaluadoEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EvaluadoResponse>> crear(@Valid @RequestBody EvaluadoRequest request) {
        EvaluadoEntity creado = service.crear(toEntity(request));

        return ResponseEntity.
                status(HttpStatus.CREATED)
                .body(ResponseBuilder
                        .success(
                                "Evaluado creado exitosamente",
                                toResponse(creado))
                );
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluadoResponse>>> listarTodos(
            @RequestParam(required = false) String numeroIdentificacion) {

        if (numeroIdentificacion != null && !numeroIdentificacion.isBlank()) {
            EvaluadoResponse encontrado = toResponse(
                    service.obtenerPorNumeroIdentificacion(numeroIdentificacion));
            return ResponseEntity.ok(ResponseBuilder.success("Evaluado encontrado", List.of(encontrado)));
        }

        List<EvaluadoResponse> data = service.listarTodos().stream()
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
