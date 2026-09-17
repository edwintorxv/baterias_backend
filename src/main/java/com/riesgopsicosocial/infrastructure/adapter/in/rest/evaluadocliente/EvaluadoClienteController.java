package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.EvaluadoClienteRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.EvaluadoClienteResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/evaluado-clientes")
public class EvaluadoClienteController {

    private final EvaluadoClienteService service;

    public EvaluadoClienteController(EvaluadoClienteService service) {
        this.service = service;
    }

    private EvaluadoClienteResponse toResponse(EvaluadoClienteEntity evaluadoClienteEntity) {
        return new EvaluadoClienteResponse(
                evaluadoClienteEntity.getId(),
                evaluadoClienteEntity.getFkEvaluado(),
                evaluadoClienteEntity.getFkCliente(),
                evaluadoClienteEntity.getFkCiudadTrabajo(),
                evaluadoClienteEntity.getAntiguedadEmpresa(),
                evaluadoClienteEntity.getNombreCargo(),
                evaluadoClienteEntity.getFkTipoCargo(),
                evaluadoClienteEntity.getFkTiempoCargo(),
                evaluadoClienteEntity.getNombreArea(),
                evaluadoClienteEntity.getFkTipoContrato(),
                evaluadoClienteEntity.getFkHorasLabor(),
                evaluadoClienteEntity.getFkTipoSalario(),
                evaluadoClienteEntity.getActivo()
        );
    }

    private EvaluadoClienteEntity toEntity(EvaluadoClienteRequest evaluadoClienteRequest) {
        EvaluadoClienteEntity evaluadoClienteEntity = new EvaluadoClienteEntity();
        evaluadoClienteEntity.setFkEvaluado(evaluadoClienteRequest.fkEvaluado());
        evaluadoClienteEntity.setFkCliente(evaluadoClienteRequest.fkCliente());
        evaluadoClienteEntity.setFkCiudadTrabajo(evaluadoClienteRequest.fkCiudadTrabajo());
        evaluadoClienteEntity.setAntiguedadEmpresa(evaluadoClienteRequest.antiguedadEmpresa());
        evaluadoClienteEntity.setNombreCargo(evaluadoClienteRequest.nombreCargo());
        evaluadoClienteEntity.setFkTipoCargo(evaluadoClienteRequest.fkTipoCargo());
        evaluadoClienteEntity.setFkTiempoCargo(evaluadoClienteRequest.fkTiempoCargo());
        evaluadoClienteEntity.setNombreArea(evaluadoClienteRequest.nombreArea());
        evaluadoClienteEntity.setFkTipoContrato(evaluadoClienteRequest.fkTipoContrato());
        evaluadoClienteEntity.setFkHorasLabor(evaluadoClienteRequest.fkHorasLabor());
        evaluadoClienteEntity.setFkTipoSalario(evaluadoClienteRequest.fkTipoSalario());
        evaluadoClienteEntity.setActivo(evaluadoClienteRequest.activo());
        return evaluadoClienteEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EvaluadoClienteResponse>> crear(@Valid @RequestBody EvaluadoClienteRequest request) {
        EvaluadoClienteEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder
                        .success(
                                "Relación evaluado-cliente creada exitosamente",
                                toResponse(creado))
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadoClienteResponse>> actualizar(@PathVariable Long id, @Valid
    @RequestBody EvaluadoClienteRequest request) {
        EvaluadoClienteEntity actualizado = service.actualizar(id, toEntity(request));

        return ResponseEntity.ok(
                ResponseBuilder
                        .success(
                                "Relación evaluado-cliente actualizada exitosamente",
                                toResponse(actualizado))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluadoClienteResponse>>> listarTodos(@RequestParam(required = false)
                                                                                  Long fkEvaluado,
                                                                                  @RequestParam(required = false)
                                                                                  Long fkCliente) {

        List<EvaluadoClienteEntity> resultado;

        if (fkEvaluado != null) {
            resultado = service.listarPorEvaluado(fkEvaluado);
        } else if (fkCliente != null) {
            resultado = service.listarPorCliente(fkCliente);
        } else {
            resultado = service.listarTodos();
        }

        List<EvaluadoClienteResponse> data = resultado.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder
                        .success(
                                "Listado de relaciones evaluado-cliente",
                                data)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadoClienteResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder
                        .success(
                                "Relación evaluado-cliente encontrada",
                                toResponse(service.obtenerPorId(id)))
        );
    }


}
