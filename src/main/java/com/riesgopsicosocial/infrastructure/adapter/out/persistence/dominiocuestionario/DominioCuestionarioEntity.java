package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "dominio_cuestionario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DominioCuestionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dominio", nullable = false)
    private Long fkDominio;

    @Column(name = "fk_cuestionario", nullable = false)
    private Long fkCuestionario;

    @Column(name = "factor_transformacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal factorTransformacion;

}
