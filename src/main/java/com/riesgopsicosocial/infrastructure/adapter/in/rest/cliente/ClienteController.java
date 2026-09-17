package com.riesgopsicosocial.infrastructure.adapter.in.rest.cliente;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.cliente.dto.ClienteRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.cliente.dto.ClienteResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    private ClienteResponse toResponse(ClienteEntity e) {
        return new ClienteResponse(
                e.getId(), e.getFkIndustria(), e.getFkCiudadMunicipio(),
                e.getNit(), e.getNombre(), e.getDireccion(), e.getTelefono(),
                e.getNombreContacto(), e.getCargoContacto(), e.getTelefonoContacto()
        );
    }

    private ClienteEntity toEntity(ClienteRequest clienteRequest) {
        ClienteEntity clienteEntity = new ClienteEntity();

        clienteEntity.setFkIndustria(clienteRequest.fkIndustria());
        clienteEntity.setFkCiudadMunicipio(clienteRequest.fkCiudadMunicipio());
        clienteEntity.setNit(clienteRequest.nit());
        clienteEntity.setNombre(clienteRequest.nombre());
        clienteEntity.setDireccion(clienteRequest.direccion());
        clienteEntity.setTelefono(clienteRequest.telefono());
        clienteEntity.setNombreContacto(clienteRequest.nombreContacto());
        clienteEntity.setCargoContacto(clienteRequest.cargoContacto());
        clienteEntity.setTelefonoContacto(clienteRequest.telefonoContacto());

        return clienteEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Cliente creado exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest request) {

        ClienteEntity actualizado = service.actualizar(id, toEntity(request));

        return ResponseEntity.ok(ResponseBuilder.success("Cliente actualizado exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listarTodos() {
        List<ClienteResponse> data = service.listarTodos()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList()
                );

        return ResponseEntity.ok(ResponseBuilder.success("Listado de clientes", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerPorId(@PathVariable Long id) {

        return ResponseEntity.ok(ResponseBuilder.success(
                "Cliente encontrado", toResponse(service.obtenerPorId(id))
        ));

    }


}
