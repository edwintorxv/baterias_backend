package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "baremo_dimension")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaremoDimensionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dimension_cuestionario", nullable = false)
    private Long fkDimensionCuestionario;

    @Column(name = "fk_nivel_riesgo", nullable = false)
    private Long fkNivelRiesgo;

    @Column(name = "valor_minimo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMaximo;

}
