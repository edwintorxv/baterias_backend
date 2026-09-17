package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension;

import com.riesgopsicosocial.shared.catalogo.CatalogoConPadreCrudService;
import com.riesgopsicosocial.shared.catalogo.ConPadre;
import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dimension")
@NoArgsConstructor
public class DimensionEntity implements Identificable, Nombrable, ConPadre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dominio", nullable = false)
    private Long fkDominio;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String nombre;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public Long getIdPadre() {
        return fkDominio;
    }

    public Long getFkDominio() {
        return fkDominio;
    }

    public void setFkDominio(Long fkDominio) {
        this.fkDominio = fkDominio;
    }


}
