package com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.usuario.dto.*;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Usuarios de la API y clientes asignados a cada psicólogo. Sin DELETE: se desactivan. */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    private UsuarioResponse toResponse(UsuarioEntity usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getRol(),
                usuario.getFkEvaluador(), usuario.getFkCliente(), usuario.getFkEvaluado(), usuario.getActivo());
    }

    private static UsuarioEntity toEntity(String correo, String nombre, Rol rol, Long idEvaluador, Long idCliente,
                                          Long idEvaluado, Boolean activo) {
        UsuarioEntity entidad = new UsuarioEntity();
        entidad.setCorreo(correo);
        entidad.setNombre(nombre);
        entidad.setRol(rol);
        entidad.setFkEvaluador(idEvaluador);
        entidad.setFkCliente(idCliente);
        entidad.setFkEvaluado(idEvaluado);
        entidad.setActivo(activo);
        return entidad;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioCrearRequest request) {
        UsuarioEntity creado = service.crear(toEntity(request.correo(), request.nombre(), request.rol(),
                request.idEvaluador(), request.idCliente(), request.idEvaluado(), request.activo()), request.contrasena());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Usuario creado exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        UsuarioEntity actualizado = service.actualizar(id, toEntity(request.correo(), request.nombre(), request.rol(),
                request.idEvaluador(), request.idCliente(), request.idEvaluado(), request.activo()));
        return ResponseEntity.ok(ResponseBuilder.success("Usuario actualizado exitosamente", toResponse(actualizado)));
    }

    @PutMapping("/{id}/contrasena")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarContrasena(
            @PathVariable Long id, @Valid @RequestBody ContrasenaRequest request) {
        UsuarioEntity actualizado = service.cambiarContrasena(id, request.contrasena());
        return ResponseEntity.ok(ResponseBuilder.success("Contraseña actualizada exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo) {
        if (correo != null && !correo.isBlank()) {
            return ResponseEntity.ok(ResponseBuilder.success("Usuario encontrado",
                    List.of(toResponse(service.obtenerPorCorreo(correo)))));
        }
        List<UsuarioResponse> data = service.listar(rol, activo).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ResponseBuilder.success("Listado de usuarios", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseBuilder.success("Usuario encontrado", toResponse(service.obtenerPorId(id))));
    }

    @GetMapping("/{id}/clientes")
    public ResponseEntity<ApiResponse<List<Long>>> listarClientes(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseBuilder.success("Clientes asignados", service.listarClientes(id)));
    }

    @PutMapping("/{id}/clientes")
    public ResponseEntity<ApiResponse<List<Long>>> asignarClientes(
            @PathVariable Long id, @Valid @RequestBody ClientesAsignadosRequest request) {
        return ResponseEntity.ok(ResponseBuilder.success("Clientes asignados exitosamente",
                service.asignarClientes(id, request.idsCliente())));
    }

}
