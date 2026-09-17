package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario;

import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuestionario")
@NoArgsConstructor
public class CuestionarioEntity implements Identificable, Nombrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "forma", nullable = false, unique = true, length = 100)
    private String forma;

    @Column(name = "descripcion", length = 500)
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

    public String getForma() {
        return forma;
    }

    public void setForma(String forma) {
        this.forma = forma;
    }

}
