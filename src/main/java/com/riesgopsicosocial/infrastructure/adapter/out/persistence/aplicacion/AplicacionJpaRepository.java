package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AplicacionJpaRepository extends JpaRepository<AplicacionEntity, Long> {

    List<AplicacionEntity> findByFkEvaluadoCliente(Long fkEvaluadoCliente);

    List<AplicacionEntity> findByFkEvaluadoClienteIn(Collection<Long> fksEvaluadoCliente);

    /** Aplicaciones de las relaciones indicadas con fecha en [desde, hasta). */
    @Query("select a from AplicacionEntity a where a.fkEvaluadoCliente in :fksEvaluadoCliente"
            + " and a.fechaAplicacion >= :desde and a.fechaAplicacion < :hasta")
    List<AplicacionEntity> buscarPorRelacionesYPeriodo(@Param("fksEvaluadoCliente") Collection<Long> fksEvaluadoCliente,
                                                       @Param("desde") LocalDateTime desde,
                                                       @Param("hasta") LocalDateTime hasta);

    List<AplicacionEntity> findByEstado(String estado);

    /**
     * Aplicaciones de los clientes indicados (y del año, si viene) con la persona, la empresa, la forma
     * intralaboral, el firmante y el avance: preguntas que aplican (según las preguntas filtro) de los
     * cuestionarios del grupo y cuántas de ellas tienen respuesta. Misma regla que el formulario.
     */
    @Query(value = """
            WITH grupo_cuestionario AS (
                SELECT DISTINCT fk_grupo_ocupacional, fk_cuestionario FROM baremo_cuestionario
            ),
            aplicaciones AS (
                SELECT a.*
                FROM aplicacion a
                JOIN evaluado_cliente ec ON ec.id = a.fk_evaluado_cliente
                WHERE ec.fk_cliente IN (:fksCliente)
                  AND (CAST(:anio AS INTEGER) IS NULL OR EXTRACT(YEAR FROM a.fecha_aplicacion) = CAST(:anio AS INTEGER))
            ),
            avance AS (
                SELECT a.id, COUNT(p.id) AS aplicables, COUNT(r.id) AS respondidas
                FROM aplicaciones a
                JOIN grupo_cuestionario gc ON gc.fk_grupo_ocupacional = a.fk_grupo_ocupacional
                JOIN dimension_cuestionario dc ON dc.fk_cuestionario = gc.fk_cuestionario
                JOIN pregunta p ON p.fk_dimension_cuestionario = dc.id
                LEFT JOIN respuesta r ON r.fk_aplicacion = a.id AND r.fk_pregunta = p.id
                WHERE dc.condicion_aplicacion IS NULL
                   OR (dc.condicion_aplicacion = 'ATIENDE_CLIENTES' AND a.atiende_clientes IS DISTINCT FROM FALSE)
                   OR (dc.condicion_aplicacion = 'ES_JEFE' AND a.es_jefe IS DISTINCT FROM FALSE)
                GROUP BY a.id
            )
            SELECT a.id                    AS "id",
                   a.fecha_aplicacion      AS "fechaAplicacion",
                   c.id                    AS "idCliente",
                   c.nombre                AS "cliente",
                   ec.id                   AS "idRelacion",
                   ec.activo               AS "relacionActiva",
                   e.id                    AS "idEvaluado",
                   e.numero_identificacion AS "numeroIdentificacion",
                   e.nombre                AS "nombre",
                   e.apellido              AS "apellido",
                   (SELECT cu.forma
                    FROM grupo_cuestionario gc
                    JOIN cuestionario cu ON cu.id = gc.fk_cuestionario
                    WHERE gc.fk_grupo_ocupacional = a.fk_grupo_ocupacional
                      AND cu.metodo_calculo = 'SUMA_POR_DOMINIOS'
                    LIMIT 1)               AS "forma",
                   ev.id                   AS "idEvaluador",
                   ev.nombre               AS "evaluador",
                   COALESCE(av.aplicables, 0)  AS "aplicables",
                   COALESCE(av.respondidas, 0) AS "respondidas",
                   EXISTS (SELECT 1 FROM respuesta r WHERE r.fk_aplicacion = a.id) AS "conRespuestas",
                   EXISTS (SELECT 1 FROM resultado_cuestionario rc WHERE rc.fk_aplicacion = a.id) AS "conResultados"
            FROM aplicaciones a
            JOIN evaluado_cliente ec ON ec.id = a.fk_evaluado_cliente
            JOIN evaluado e ON e.id = ec.fk_evaluado
            JOIN cliente c ON c.id = ec.fk_cliente
            LEFT JOIN evaluador ev ON ev.id = a.fk_evaluador
            LEFT JOIN avance av ON av.id = a.id
            ORDER BY a.fecha_aplicacion DESC, a.id DESC
            """, nativeQuery = true)
    List<AplicacionListadoFila> listado(@Param("fksCliente") Collection<Long> fksCliente, @Param("anio") Integer anio);

}
