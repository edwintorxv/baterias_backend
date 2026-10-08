package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluador;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluador.dto.EvaluadorRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluador.dto.EvaluadorResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorService;
import com.riesgopsicosocial.shared.exception.ValidationException;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/evaluadores")
public class EvaluadorController {

    private final EvaluadorService service;

    public EvaluadorController(EvaluadorService service) {
        this.service = service;
    }

    private EvaluadorResponse toResponse(EvaluadorEntity evaluadorEntity) {
        return new EvaluadorResponse(
                evaluadorEntity.getId(),
                evaluadorEntity.getNumeroIdentificacion(),
                evaluadorEntity.getNombre(),
                evaluadorEntity.getProfesion(),
                evaluadorEntity.getPosgrado(),
                evaluadorEntity.getTarjetaProfesional(),
                evaluadorEntity.getLicenciaSaludOcupacional(),
                evaluadorEntity.getFechaExpedicionLicencia(),
                evaluadorEntity.getActivo(),
                evaluadorEntity.getFirma() != null
        );
    }

    private EvaluadorEntity toEntity(EvaluadorRequest evaluadorRequest) {
        EvaluadorEntity evaluadorEntity = new EvaluadorEntity();
        evaluadorEntity.setNumeroIdentificacion(evaluadorRequest.numeroIdentificacion());
        evaluadorEntity.setNombre(evaluadorRequest.nombre());
        evaluadorEntity.setProfesion(evaluadorRequest.profesion());
        evaluadorEntity.setPosgrado(evaluadorRequest.posgrado());
        evaluadorEntity.setTarjetaProfesional(evaluadorRequest.tarjetaProfesional());
        evaluadorEntity.setLicenciaSaludOcupacional(evaluadorRequest.licenciaSaludOcupacional());
        evaluadorEntity.setFechaExpedicionLicencia(evaluadorRequest.fechaExpedicionLicencia());
        evaluadorEntity.setActivo(evaluadorRequest.activo());
        return evaluadorEntity;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EvaluadorResponse>> crear(@Valid @RequestBody EvaluadorRequest request) {
        EvaluadorEntity creado = service.crear(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success("Evaluador creado exitosamente", toResponse(creado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadorResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody EvaluadorRequest request) {
        EvaluadorEntity actualizado = service.actualizar(id, toEntity(request));
        return ResponseEntity.ok(ResponseBuilder.success("Evaluador actualizado exitosamente", toResponse(actualizado)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluadorResponse>>> listarTodos(
            @RequestParam(required = false) String numeroIdentificacion,
            @RequestParam(required = false) Boolean activo) {

        if (numeroIdentificacion != null && !numeroIdentificacion.isBlank()) {
            EvaluadorResponse encontrado = toResponse(service.obtenerPorNumeroIdentificacion(numeroIdentificacion));
            return ResponseEntity.ok(ResponseBuilder.success("Evaluador encontrado", List.of(encontrado)));
        }

        List<EvaluadorEntity> resultado = activo != null ? service.listarPorActivo(activo) : service.listarTodos();
        List<EvaluadorResponse> data = resultado.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ResponseBuilder.success("Listado de evaluadores", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluadorResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseBuilder.success("Evaluador encontrado", toResponse(service.obtenerPorId(id))));
    }

    /** Imagen PNG o JPG de hasta 1 MB, en el campo multipart {@code archivo}. */
    @PutMapping(value = "/{id}/firma", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EvaluadorResponse>> guardarFirma(
            @PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        byte[] imagen;
        try {
            imagen = archivo.getBytes();
        } catch (IOException e) {
            throw new ValidationException("No se pudo leer el archivo de la firma", List.of(e.getMessage()));
        }
        EvaluadorEntity actualizado = service.guardarFirma(id, imagen, archivo.getContentType());
        return ResponseEntity.ok(ResponseBuilder.success("Firma guardada exitosamente", toResponse(actualizado)));
    }

    @GetMapping("/{id}/firma")
    public ResponseEntity<byte[]> obtenerFirma(@PathVariable Long id) {
        EvaluadorEntity evaluador = service.obtenerConFirma(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evaluador.getFirmaTipoContenido()))
                .body(evaluador.getFirma());
    }

}
