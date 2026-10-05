package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "baremo_cuestionario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaremoCuestionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_cuestionario", nullable = false)
    private Long fkCuestionario;

    @Column(name = "fk_nivel_riesgo", nullable = false)
    private Long fkNivelRiesgo;

    @Column(name = "fk_grupo_ocupacional", nullable = false)
    private Long fkGrupoOcupacional;

    @Column(name = "valor_minimo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMaximo;

}
