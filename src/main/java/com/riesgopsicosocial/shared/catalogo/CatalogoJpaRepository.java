package com.riesgopsicosocial.shared.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CatalogoJpaRepository<T> extends JpaRepository<T, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

}
