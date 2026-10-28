package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface RegistroIngresoJpaRepository extends JpaRepository<RegistroIngresoEntity, Long> {

    /** Devuelve cuántas filas borró. */
    @Transactional
    @Modifying
    @Query("DELETE FROM RegistroIngresoEntity r WHERE r.fecha < :limite")
    int borrarAnterioresA(LocalDateTime limite);

}
