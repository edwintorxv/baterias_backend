package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluadoClienteJpaRepository extends JpaRepository<EvaluadoClienteEntity, Long> {

    List<EvaluadoClienteEntity> findByFkEvaluado(Long fkEvaluado);

    List<EvaluadoClienteEntity> findByFkCliente(Long fkCliente);

    List<EvaluadoClienteEntity> findByFkClienteIn(Collection<Long> fksCliente);

    List<EvaluadoClienteEntity> findByFkEvaluadoAndFkCliente(Long fkEvaluado, Long fkCliente);

    boolean existsByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);

    Optional<EvaluadoClienteEntity> findByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);

    /** Personas del cliente (activas e inactivas) con la última aplicación de cada relación. */
    @Query(value = """
            SELECT ec.id                     AS "idRelacion",
                   e.id                      AS "idEvaluado",
                   e.numero_identificacion   AS "numeroIdentificacion",
                   e.nombre                  AS "nombre",
                   e.apellido                AS "apellido",
                   ec.nombre_cargo           AS "nombreCargo",
                   tc.nombre                 AS "tipoCargo",
                   ec.nombre_area            AS "nombreArea",
                   cm.nombre                 AS "ciudadTrabajo",
                   ec.activo                 AS "activo",
                   (SELECT COUNT(*) FROM aplicacion a WHERE a.fk_evaluado_cliente = ec.id) AS "aplicaciones",
                   ua.id                     AS "idUltimaAplicacion",
                   ua.fecha_aplicacion       AS "fechaUltimaAplicacion",
                   ua.estado                 AS "estadoUltimaAplicacion",
                   EXISTS (SELECT 1 FROM respuesta r WHERE r.fk_aplicacion = ua.id) AS "ultimaConRespuestas",
                   EXISTS (SELECT 1 FROM resultado_cuestionario rc WHERE rc.fk_aplicacion = ua.id) AS "ultimaConResultados"
            FROM evaluado_cliente ec
            JOIN evaluado e ON e.id = ec.fk_evaluado
            LEFT JOIN tipo_cargo tc ON tc.id = ec.fk_tipo_cargo
            LEFT JOIN ciudad_municipio cm ON cm.id = ec.fk_ciudad_trabajo
            LEFT JOIN LATERAL (
                SELECT a.id, a.fecha_aplicacion, a.estado
                FROM aplicacion a
                WHERE a.fk_evaluado_cliente = ec.id
                ORDER BY a.fecha_aplicacion DESC, a.id DESC
                LIMIT 1
            ) ua ON TRUE
            WHERE ec.fk_cliente = :fkCliente
            ORDER BY e.apellido, e.nombre, ec.id
            """, nativeQuery = true)
    List<PersonaClienteFila> listarPersonasPorCliente(@Param("fkCliente") Long fkCliente);

}
