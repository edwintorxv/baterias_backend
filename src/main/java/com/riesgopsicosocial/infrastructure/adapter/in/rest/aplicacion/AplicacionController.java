package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.AplicacionListadoResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.AplicacionRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.AplicacionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionListadoFila;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.EstadoAplicacion;
import com.riesgopsicosocial.infrastructure.security.AlcanceUsuario;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/aplicaciones")
public class AplicacionController {

    private final AplicacionService service;
    private final AlcanceUsuario alcance;

    public AplicacionController(AplicacionService service, AlcanceUsuario alcance) {
        this.service = service;
        this.alcance = alcance;
    }

    private AplicacionResponse toResponse(AplicacionEntity aplicacionEntity) {
        return new AplicacionResponse(
                aplicacionEntity.getId(),
                aplicacionEntity.getFkEvaluadoCliente(),
                aplicacionEntity.getFkGrupoOcupacional(),
                aplicacionEntity.getFechaAplicacion(),
                aplicacionEntity.getObservaciones(),
                aplicacionEntity.getRecomendaciones(),
                aplicacionEntity.getEstado(),
                aplicacionEntity.getAtiendeClientes(),
                aplicacionEntity.getEsJefe(),
                aplicacionEntity.getFkEvaluador()
        );
    }

    private AplicacionEntity toEntity(AplicacionRequest aplicacionRequest) {
        AplicacionEntity aplicacionEntity = new AplicacionEntity();
        aplicacionEntity.setFkEvaluadoCliente(aplicacionRequest.fkEvaluadoCliente());
        aplicacionEntity.setFkGrupoOcupacional(aplicacionRequest.fkGrupoOcupacional());
        aplicacionEntity.setFechaAplicacion(aplicacionRequest.fechaAplicacion());
        aplicacionEntity.setObservaciones(aplicacionRequest.observaciones());
        aplicacionEntity.setEstado(aplicacionRequest.estado());
        aplicacionEntity.setAtiendeClientes(aplicacionRequest.atiendeClientes());
        aplicacionEntity.setEsJefe(aplicacionRequest.esJefe());
        aplicacionEntity.setRecomendaciones(aplicacionRequest.recomendaciones());
        aplicacionEntity.setFkEvaluador(aplicacionRequest.fkEvaluador());
        return aplicacionEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AplicacionResponse>> crear(@Valid @RequestBody AplicacionRequest request) {
        alcance.exigirRelacion(request.fkEvaluadoCliente());
        alcance.exigirFirmante(request.fkEvaluador(), null);
        AplicacionEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(
                        "Aplicación creada exitosamente",
                        toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AplicacionResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody AplicacionRequest request) {
        alcance.exigirRelacion(request.fkEvaluadoCliente()); // la aplicación actual la revisa AlcanceClienteInterceptor
        alcance.exigirFirmante(request.fkEvaluador(), service.obtenerPorId(id).getFkEvaluador());
        AplicacionEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Aplicación actualizada exitosamente",
                        toResponse(actualizado))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AplicacionResponse>>> listarTodos(
            @RequestParam(required = false) Long fkEvaluadoCliente,
            @RequestParam(required = false) String estado) {

        List<AplicacionEntity> resultado;
        if (fkEvaluadoCliente != null) {
            resultado = service.listarPorEvaluadoCliente(fkEvaluadoCliente);
        } else if (estado != null && !estado.isBlank()) {
            resultado = service.listarPorEstado(estado);
        } else {
            resultado = service.listarTodos();
        }

        Optional<Set<Long>> relaciones = alcance.relacionesVisibles();
        List<AplicacionResponse> data = resultado.stream()
                .filter(a -> relaciones.map(r -> r.contains(a.getFkEvaluadoCliente())).orElse(true))
                .map(this::toResponse)
                .collect(Collectors
                        .toList());
        return ResponseEntity.ok(ResponseBuilder.success("Listado de aplicaciones", data));
    }

    /**
     * Listado general: las aplicaciones de los clientes asignados (o de uno de ellos), del año si viene,
     * con la persona, el avance y el estado calculado. Solo el psicólogo (SecurityConfig).
     */
    @GetMapping("/listado")
    public ResponseEntity<ApiResponse<List<AplicacionListadoResponse>>> listado(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Integer anio) {
        Set<Long> clientes;
        if (idCliente != null) {
            alcance.exigirCliente(idCliente);
            clientes = Set.of(idCliente);
        } else {
            clientes = alcance.clientesVisibles().orElse(Set.of()); // el ADMIN no llega aquí
        }
        List<AplicacionListadoResponse> data = service.listado(clientes, anio).stream()
                .map(AplicacionController::toListado)
                .toList();
        return ResponseEntity.ok(ResponseBuilder.success("Listado de aplicaciones", data));
    }

    private static AplicacionListadoResponse toListado(AplicacionListadoFila fila) {
        int aplicables = fila.getAplicables().intValue();
        int respondidas = fila.getRespondidas().intValue();
        return new AplicacionListadoResponse(fila.getId(), fila.getFechaAplicacion(), fila.getIdCliente(), fila.getCliente(),
                fila.getIdRelacion(), fila.getRelacionActiva(), fila.getIdEvaluado(), fila.getNumeroIdentificacion(),
                fila.getNombre(), fila.getApellido(), fila.getForma(), fila.getIdEvaluador(), fila.getEvaluador(),
                aplicables, respondidas,
                EstadoAplicacion.de(fila.getConRespuestas(), fila.getConResultados(), respondidas == aplicables));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AplicacionResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Aplicación encontrada",
                        toResponse(service.obtenerPorId(id)))
        );
    }

}
