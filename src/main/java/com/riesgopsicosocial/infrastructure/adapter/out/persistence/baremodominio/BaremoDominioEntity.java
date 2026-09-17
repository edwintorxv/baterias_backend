package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "baremo_dominio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaremoDominioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dominio_cuestionario", nullable = false)
    private Long fkDominioCuestionario;

    @Column(name = "fk_nivel_riesgo", nullable = false)
    private Long fkNivelRiesgo;

    @Column(name = "valor_minimo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorMaximo;

}
