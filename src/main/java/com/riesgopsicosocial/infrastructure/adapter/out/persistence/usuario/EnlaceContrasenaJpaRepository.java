package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnlaceContrasenaJpaRepository extends JpaRepository<EnlaceContrasenaEntity, Long> {

    Optional<EnlaceContrasenaEntity> findByCodigoHash(String codigoHash);

    /** Con bloqueo de escritura: para usarlo, que dos envíos a la vez no pasen los dos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EnlaceContrasenaEntity e where e.codigoHash = :codigoHash")
    Optional<EnlaceContrasenaEntity> buscarParaUsar(@Param("codigoHash") String codigoHash);

    /** Anula los enlaces del usuario que aún no se han usado; devuelve cuántos. */
    @Modifying
    @Query("""
            update EnlaceContrasenaEntity e
               set e.anulado = true, e.fechaModificado = :fecha, e.usuarioModifica = :quien
             where e.fkUsuario = :idUsuario and e.fechaUso is null and e.anulado = false""")
    int anularSinUsar(@Param("idUsuario") Long idUsuario, @Param("fecha") LocalDateTime fecha,
                      @Param("quien") String quien);

    /** True si ya tuvo un enlace para restablecer o usó alguno: ya no es una invitación. */
    @Query("""
            select count(e) > 0 from EnlaceContrasenaEntity e
             where e.fkUsuario = :idUsuario
               and (e.proposito = com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.PropositoEnlace.RESTABLECER
                    or e.fechaUso is not null)""")
    boolean yaTuvoContrasena(@Param("idUsuario") Long idUsuario);

    /**
     * Por usuario, el vencimiento de su enlace vigente (sin usar, sin anular y sin vencer). Cada fila es
     * {idUsuario, vence}; quien no tiene enlace vigente no aparece. Normalmente hay uno solo por usuario,
     * porque generar uno nuevo anula los anteriores.
     */
    @Query("""
            select e.fkUsuario, max(e.vence) from EnlaceContrasenaEntity e
             where e.fkUsuario in :idsUsuario and e.fechaUso is null and e.anulado = false and e.vence > :ahora
             group by e.fkUsuario""")
    List<Object[]> vencimientosVigentes(@Param("idsUsuario") Collection<Long> idsUsuario,
                                        @Param("ahora") LocalDateTime ahora);

}
