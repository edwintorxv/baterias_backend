package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroIngresoJpaRepository extends JpaRepository<RegistroIngresoEntity, Long> {

    /** Devuelve cuántas filas borró. */
    @Transactional
    @Modifying
    @Query("DELETE FROM RegistroIngresoEntity r WHERE r.fecha < :limite")
    int borrarAnterioresA(LocalDateTime limite);

    /** Intentos con fecha en [desde, hasta), del más reciente al más antiguo, con el nombre del usuario si existe. */
    @Query("""
            SELECT new com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.IngresoFila(
                       r.id, r.fecha, r.correo, r.fkUsuario, u.nombre, r.ip, r.navegador, r.resultado)
              FROM RegistroIngresoEntity r
              LEFT JOIN com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity u
                     ON u.id = r.fkUsuario
             WHERE r.fecha >= :desde AND r.fecha < :hasta
             ORDER BY r.fecha DESC, r.id DESC""")
    List<IngresoFila> listar(LocalDateTime desde, LocalDateTime hasta, Limit limite);

    /** Cuántos intentos hubo de cada resultado en [desde, hasta). Cada fila es {resultado, cantidad}. */
    @Query("""
            SELECT r.resultado, COUNT(r) FROM RegistroIngresoEntity r
             WHERE r.fecha >= :desde AND r.fecha < :hasta
             GROUP BY r.resultado""")
    List<Object[]> contarPorResultado(LocalDateTime desde, LocalDateTime hasta);

}
