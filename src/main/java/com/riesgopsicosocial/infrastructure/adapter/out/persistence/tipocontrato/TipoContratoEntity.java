package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocontrato;

import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipo_contrato")
@NoArgsConstructor
public class TipoContratoEntity implements Identificable, Nombrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 50)
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

}
