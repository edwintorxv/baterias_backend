package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto.RespuestasCuestionarioRequest;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.FormularioAplicacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestasAplicacionService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestasAplicacionService.Guardado;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestasAplicacionService.RespuestaNueva;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Digitación de una aplicación (solo el psicólogo, sobre las de sus clientes: el alcance lo revisa
 * AlcanceClienteInterceptor por {@code idAplicacion}).
 */
@RestController
@RequestMapping("/aplicaciones/{idAplicacion}")
public class RespuestasAplicacionController {

    private final RespuestasAplicacionService service;

    public RespuestasAplicacionController(RespuestasAplicacionService service) {
        this.service = service;
    }

    /** Cuestionarios que le corresponden, con sus preguntas, opciones y lo ya respondido. */
    @GetMapping("/formulario")
    public ResponseEntity<ApiResponse<FormularioAplicacion>> formulario(@PathVariable Long idAplicacion) {
        return ResponseEntity.ok(ResponseBuilder.success("Formulario de la aplicación", service.formulario(idAplicacion)));
    }

    /**
     * Reemplaza las respuestas de un cuestionario (todo o nada; 400 con un error por pregunta). Si cambian y
     * la aplicación tenía resultados, se borran y hay que volver a calcular.
     */
    @PutMapping("/respuestas/{idCuestionario}")
    public ResponseEntity<ApiResponse<FormularioAplicacion>> guardar(@PathVariable Long idAplicacion,
                                                                     @PathVariable Long idCuestionario,
                                                                     @Valid @RequestBody RespuestasCuestionarioRequest request) {
        Guardado guardado = service.guardar(idAplicacion, idCuestionario, request.respuestas().stream()
                .map(r -> new RespuestaNueva(r.fkPregunta(), r.fkOpcionRespuesta()))
                .toList());
        String mensaje = !guardado.huboCambios() ? "Sin cambios en las respuestas"
                : guardado.resultadosBorrados()
                ? "Respuestas guardadas. Los resultados anteriores se borraron: vuelva a calcular"
                : "Respuestas guardadas";
        return ResponseEntity.ok(ResponseBuilder.success(mensaje, guardado.formulario()));
    }

}
