package com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.EstructuraCuestionario;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.EstructuraCuestionarioService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuración de los cuestionarios en solo lectura (ADMIN y PSICOLOGO, como el resto de la configuración):
 * la estructura completa de cada uno y los baremos del total general.
 */
@RestController
@RequestMapping("/cuestionarios")
public class EstructuraCuestionarioController {

    private final EstructuraCuestionarioService service;

    public EstructuraCuestionarioController(EstructuraCuestionarioService service) {
        this.service = service;
    }

    /** Dominios → dimensiones → preguntas, escalas con sus valores y baremos por grupo ocupacional. */
    @GetMapping("/{id}/estructura")
    public ResponseEntity<ApiResponse<EstructuraCuestionario>> estructura(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseBuilder.success("Estructura del cuestionario", service.estructura(id)));
    }

    /** Baremos del total general (intralaboral + extralaboral), uno por forma intralaboral. */
    @GetMapping("/total-general")
    public ResponseEntity<ApiResponse<List<EstructuraCuestionario.TotalGeneral>>> totalGeneral() {
        return ResponseEntity.ok(ResponseBuilder.success("Baremos del total general", service.totalGeneral()));
    }

}
