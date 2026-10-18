package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByUsuarioIgnoreCase(String usuario);

    Optional<UsuarioEntity> findByUsuarioIgnoreCase(String usuario);

    boolean existsByFkEvaluador(Long fkEvaluador);

    boolean existsByFkEvaluado(Long fkEvaluado);

    List<UsuarioEntity> findByRol(Rol rol);

    List<UsuarioEntity> findByActivo(Boolean activo);

    List<UsuarioEntity> findByRolAndActivo(Rol rol, Boolean activo);

}
