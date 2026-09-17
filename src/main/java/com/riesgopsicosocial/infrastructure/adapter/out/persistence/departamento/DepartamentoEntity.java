package com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento;

import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departamento")
@NoArgsConstructor
public class DepartamentoEntity implements Identificable, Nombrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_dane", unique = true, length = 10)
    private String codigoDane;

    @Column(name = "nombre", nullable = false, length = 150)
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

    public String getCodigoDane() {

        return codigoDane;
    }

    public void setCodigoDane(String codigoDane) {

        this.codigoDane = codigoDane;
    }

}
