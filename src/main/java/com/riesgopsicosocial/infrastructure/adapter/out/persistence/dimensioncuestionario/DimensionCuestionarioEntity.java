package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "dimension_cuestionario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DimensionCuestionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dimension", nullable = false)
    private Long fkDimension;

    @Column(name = "fk_cuestionario", nullable = false)
    private Long fkCuestionario;

    @Column(name = "factor_transformacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal factorTransformacion;

}
