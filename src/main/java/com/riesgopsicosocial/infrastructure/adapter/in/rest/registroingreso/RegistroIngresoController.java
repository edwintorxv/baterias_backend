package com.riesgopsicosocial.infrastructure.adapter.in.rest.registroingreso;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.ConsultaIngresos;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.RegistroIngresoService;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Intentos de ingreso para el administrador (solo ADMIN: la IP es un dato personal). Solo lectura y sin
 * exportar: los registros se borran solos a los 12 meses.
 */
@RestController
@RequestMapping("/registro-ingresos")
public class RegistroIngresoController {

    private final RegistroIngresoService service;

    public RegistroIngresoController(RegistroIngresoService service) {
        this.service = service;
    }

    /** Fechas yyyy-MM-dd, las dos incluidas; sin ellas, los últimos 30 días. */
    @GetMapping
    public ResponseEntity<ApiResponse<ConsultaIngresos>> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ConsultaIngresos consulta = service.consultar(desde, hasta);
        String mensaje = consulta.truncado()
                ? "Se muestran los " + RegistroIngresoService.MAXIMO_FILAS + " intentos más recientes: acorte el rango"
                : "Intentos de ingreso";
        return ResponseEntity.ok(ResponseBuilder.success(mensaje, consulta));
    }
}
