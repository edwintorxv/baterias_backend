package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.PersonaClienteResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.PersonaClienteFila;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Personas de un cliente en una sola consulta (evaluado + relación + última aplicación). Solo el
 * psicólogo; el alcance por cliente lo revisa AlcanceClienteInterceptor (cliente ajeno → 404).
 */
@RestController
@RequestMapping("/clientes/{idCliente}/personas")
public class PersonaClienteController {

    private final EvaluadoClienteService service;

    public PersonaClienteController(EvaluadoClienteService service) {
        this.service = service;
    }

    private static PersonaClienteResponse toResponse(PersonaClienteFila fila) {
        PersonaClienteResponse.UltimaAplicacion ultima = fila.getIdUltimaAplicacion() == null ? null
                : new PersonaClienteResponse.UltimaAplicacion(fila.getIdUltimaAplicacion(),
                fila.getFechaUltimaAplicacion(), fila.getEstadoUltimaAplicacion(),
                Boolean.TRUE.equals(fila.getUltimaConRespuestas()), Boolean.TRUE.equals(fila.getUltimaConResultados()));
        return new PersonaClienteResponse(fila.getIdRelacion(), fila.getIdEvaluado(), fila.getNumeroIdentificacion(),
                fila.getNombre(), fila.getApellido(), fila.getNombreCargo(), fila.getTipoCargo(), fila.getNombreArea(),
                fila.getCiudadTrabajo(), fila.getActivo(), fila.getAplicaciones() == null ? 0 : fila.getAplicaciones(),
                ultima);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonaClienteResponse>>> listar(@PathVariable Long idCliente) {
        List<PersonaClienteResponse> data = service.listarPersonasPorCliente(idCliente).stream()
                .map(PersonaClienteController::toResponse)
                .toList();
        return ResponseEntity.ok(ResponseBuilder.success("Personas del cliente", data));
    }

}
