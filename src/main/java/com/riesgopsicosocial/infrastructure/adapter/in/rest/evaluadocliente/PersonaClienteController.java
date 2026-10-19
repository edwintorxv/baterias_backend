package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.PersonaClienteResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.VincularPersonaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.VinculacionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.PersonaClienteFila;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.ResultadoVinculacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.Vinculacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.VinculacionPersonaService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Personas de un cliente: listado en una sola consulta (evaluado + relación + última aplicación) y
 * vinculación (la única forma de crear personas y relaciones). Solo el psicólogo; el alcance por cliente
 * lo revisa AlcanceClienteInterceptor (cliente ajeno → 404).
 */
@RestController
@RequestMapping("/clientes/{idCliente}/personas")
public class PersonaClienteController {

    private final EvaluadoClienteService service;
    private final VinculacionPersonaService vinculacionService;

    public PersonaClienteController(EvaluadoClienteService service, VinculacionPersonaService vinculacionService) {
        this.service = service;
        this.vinculacionService = vinculacionService;
    }

    private static EvaluadoEntity toPersona(VincularPersonaRequest request) {
        EvaluadoEntity persona = new EvaluadoEntity();
        persona.setNumeroIdentificacion(request.numeroIdentificacion());
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setFkSexo(request.fkSexo());
        persona.setAnioNacimiento(request.anioNacimiento());
        return persona;
    }

    private static EvaluadoClienteEntity toRelacion(VincularPersonaRequest request) {
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setFkEstadoCivil(request.fkEstadoCivil());
        relacion.setFkNivelAcademico(request.fkNivelAcademico());
        relacion.setFkCiudadResidencia(request.fkCiudadResidencia());
        relacion.setFkEstratoSocioeconomico(request.fkEstratoSocioeconomico());
        relacion.setFkTipoVivienda(request.fkTipoVivienda());
        relacion.setOcupacionProfesion(request.ocupacionProfesion());
        relacion.setFamiliaresDependientesEconomicamente(request.familiaresDependientesEconomicamente());
        relacion.setFkCiudadTrabajo(request.fkCiudadTrabajo());
        relacion.setAntiguedadEmpresa(request.antiguedadEmpresa());
        relacion.setNombreCargo(request.nombreCargo());
        relacion.setFkTipoCargo(request.fkTipoCargo());
        relacion.setFkTiempoCargo(request.fkTiempoCargo());
        relacion.setNombreArea(request.nombreArea());
        relacion.setFkTipoContrato(request.fkTipoContrato());
        relacion.setFkHorasLabor(request.fkHorasLabor());
        relacion.setFkTipoSalario(request.fkTipoSalario());
        relacion.setFechaVinculacion(request.fechaVinculacion());
        return relacion;
    }

    /** No dice en qué empresa estaba: el psicólogo de este cliente no tiene por qué saberlo. */
    private static String mensaje(Vinculacion vinculacion) {
        String mensaje = switch (vinculacion.resultado()) {
            case NUEVA -> "Persona creada y vinculada al cliente";
            case VINCULA -> "Persona vinculada al cliente";
            case ACTUALIZA -> "La persona ya estaba vinculada al cliente: se actualizaron sus datos";
            case CAMBIA_EMPRESA -> "Persona vinculada al cliente; tenía vínculo activo en otra empresa y se inactivó";
        };
        return vinculacion.datosPersonaActualizados() && vinculacion.resultado() != ResultadoVinculacion.ACTUALIZA
                ? mensaje + " (se actualizaron sus datos personales)"
                : mensaje;
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

    /**
     * Crea o reutiliza la persona (por documento) y la deja activa en este cliente: 201 si se creó la
     * relación, 200 si ya estaba activa aquí y solo se actualizó.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VinculacionResponse>> vincular(@PathVariable Long idCliente,
                                                                     @Valid @RequestBody VincularPersonaRequest request) {
        Vinculacion vinculacion = vinculacionService.vincular(idCliente, toPersona(request), toRelacion(request));
        VinculacionResponse data = new VinculacionResponse(vinculacion.resultado().name(), vinculacion.evaluado().getId(),
                vinculacion.datosPersonaActualizados(), EvaluadoClienteController.toResponse(vinculacion.relacion()));
        HttpStatus estado = vinculacion.resultado() == ResultadoVinculacion.ACTUALIZA ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(ResponseBuilder.success(mensaje(vinculacion), data));
    }

}
