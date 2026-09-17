package com.riesgopsicosocial.infrastructure.adapter.in.rest.ciudadmunicipio;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.ciudadmunicipio.dto.CiudadMunicipioResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ciudades-municipios")
public class CiudadMunicipioController {

    private final CiudadMunicipioService service;

    public CiudadMunicipioController(CiudadMunicipioService service) {
        this.service = service;
    }

    private CiudadMunicipioResponse toResponse(CiudadMunicipioEntity entity) {
        return new CiudadMunicipioResponse(
                entity.getId(),
                entity.getFkDepartamento(),
                entity.getCodigoDane(),
                entity.getNombre()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CiudadMunicipioResponse>>> listar(
            @RequestParam(required = false) Long departamentoId) {

        List<CiudadMunicipioEntity> entidades = (departamentoId != null)
                ? service.listarPorDepartamento(departamentoId)
                : service.listarTodos();

        List<CiudadMunicipioResponse> data = entidades.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ResponseBuilder.success("Listado de ciudades/municipios", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CiudadMunicipioResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseBuilder.success(
                "Ciudad/Municipio encontrado", toResponse(service.obtenerPorId(id))));
    }

}
