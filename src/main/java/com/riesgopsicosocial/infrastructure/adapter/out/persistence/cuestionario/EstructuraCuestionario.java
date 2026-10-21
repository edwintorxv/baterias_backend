package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuración completa de un cuestionario, en solo lectura: dominios → dimensiones → preguntas, con sus
 * factores de transformación, las escalas con el valor de cada opción y los baremos de cada nivel por grupo
 * ocupacional. C y D tienen un único dominio que solo agrupa (no se califica: no trae baremos).
 *
 * @param baremos baremos del puntaje total del cuestionario
 * @param escalas solo las que usan sus preguntas
 */
public record EstructuraCuestionario(
        Long id,
        String forma,
        String nombre,
        MetodoCalculo metodoCalculo,
        BigDecimal factorTransformacion,
        int totalPreguntas,
        List<Baremo> baremos,
        List<Dominio> dominios,
        List<Escala> escalas
) {

    /** @param id el de la relación dominio-cuestionario */
    public record Dominio(
            Long id,
            String nombre,
            BigDecimal factorTransformacion,
            List<Baremo> baremos,
            List<Dimension> dimensiones
    ) {
    }

    /**
     * @param id                   el de la relación dimensión-cuestionario
     * @param peso                 solo en D (promedio ponderado)
     * @param condicionAplicacion  pregunta filtro de la que depende; {@code null} si siempre aplica
     * @param maxItemsSinRespuesta tolerancia del manual
     */
    public record Dimension(
            Long id,
            String nombre,
            BigDecimal factorTransformacion,
            BigDecimal peso,
            CondicionAplicacion condicionAplicacion,
            Short maxItemsSinRespuesta,
            List<Baremo> baremos,
            List<Pregunta> preguntas
    ) {
    }

    public record Pregunta(Long id, Integer numero, String descripcion, Long idEscala) {
    }

    /** @param opciones en el orden de la escala (Siempre … Nunca) */
    public record Escala(Long id, String nombre, List<Opcion> opciones) {
    }

    /** @param valor puntaje que suma la opción */
    public record Opcion(Long id, String nombre, BigDecimal valor) {
    }

    /** Rango del puntaje transformado para un nivel de riesgo y un grupo ocupacional. */
    public record Baremo(
            Long idGrupoOcupacional,
            Long idNivelRiesgo,
            String nivelRiesgo,
            BigDecimal valorMinimo,
            BigDecimal valorMaximo
    ) {
    }

    /**
     * Baremos del total general (intralaboral + extralaboral, Tabla 34), uno por forma intralaboral.
     *
     * @param factorTransformacion suma de los factores de ambos cuestionarios (no se guarda: 616 en A, 512 en B)
     */
    public record TotalGeneral(
            Long idCuestionarioIntralaboral,
            String formaIntralaboral,
            String formaExtralaboral,
            BigDecimal factorTransformacion,
            List<Baremo> baremos
    ) {
    }

}
