package com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio;

import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ciudad_municipio")
@NoArgsConstructor
public class CiudadMunicipioEntity implements Identificable, Nombrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_departamento", nullable = false)
    private Long fkDepartamento;

    @Column(name = "codigo_dane", nullable = false)
    private String codigoDane;

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

    public String getCodigoDane() {
        return codigoDane;
    }


    public void setCodigoDane(String codigoDane) {
        this.codigoDane = codigoDane;
    }


    public Long getFkDepartamento() {
        return fkDepartamento;
    }

    public void setFkDepartamento(Long fkDepartamento) {
        this.fkDepartamento = fkDepartamento;
    }


}
