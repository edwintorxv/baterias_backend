package com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria;

import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "industria")
@NoArgsConstructor
public class IndustriaEntity implements Identificable, Nombrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_sector_economico", nullable = false)
    private Long fkSectorEconomico;

    @Column(name = "nombre", nullable = false, length = 200)
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

    public Long getFkSectorEconomico() {
        return fkSectorEconomico;
    }

    public void setFkSectorEconomico(Long fkSectorEconomico) {
        this.fkSectorEconomico = fkSectorEconomico;
    }


}
