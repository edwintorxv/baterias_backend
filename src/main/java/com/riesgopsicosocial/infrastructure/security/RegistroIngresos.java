package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.RegistroIngresoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.RegistroIngresoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.ResultadoIngreso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Period;

/**
 * Registro de los intentos de inicio de sesión (tabla registro_ingreso) y su limpieza por antigüedad.
 * Registrar nunca impide ingresar: si falla la escritura, solo queda en el log.
 */
@Component
public class RegistroIngresos {

    private static final Logger log = LoggerFactory.getLogger(RegistroIngresos.class);
    static final int MAX_NAVEGADOR = 255;

    private final RegistroIngresoJpaRepository repository;
    private final Period retencion;

    public RegistroIngresos(RegistroIngresoJpaRepository repository,
                            @Value("${app.seguridad.ingresos.retencion:12m}") Period retencion) {
        this.repository = repository;
        this.retencion = retencion;
    }

    public void registrar(String correo, Long idUsuario, String ip, String navegador, ResultadoIngreso resultado) {
        try {
            repository.save(new RegistroIngresoEntity(null, null, correo, idUsuario, ip,
                    recortar(navegador), resultado));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el ingreso {} de {} desde {}: {}", resultado, correo, ip, e.getMessage());
        }
    }

    /** Al minuto de arrancar y luego una vez al día (en Render Free el servicio se duerme y reinicia). */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "P1D")
    public void borrarAntiguos() {
        int borrados = repository.borrarAnterioresA(LocalDateTime.now().minus(retencion));
        if (borrados > 0) {
            log.info("Registro de ingresos: {} filas con más de {} borradas", borrados, retencion);
        }
    }

    private static String recortar(String navegador) {
        return navegador == null || navegador.length() <= MAX_NAVEGADOR
                ? navegador : navegador.substring(0, MAX_NAVEGADOR);
    }

}
