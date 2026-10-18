package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioClienteJpaRepository extends JpaRepository<UsuarioClienteEntity, UsuarioClienteEntity.Id> {

    List<UsuarioClienteEntity> findByFkUsuario(Long fkUsuario);

    boolean existsByFkUsuarioAndFkCliente(Long fkUsuario, Long fkCliente);

    @Modifying
    @Query("delete from UsuarioClienteEntity uc where uc.fkUsuario = :fkUsuario")
    void borrarDeUsuario(@Param("fkUsuario") Long fkUsuario);

}
