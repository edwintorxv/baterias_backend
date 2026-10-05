package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultado_cuestionario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCuestionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_aplicacion", nullable = false)
    private Long fkAplicacion;

    @Column(name = "fk_cuestionario", nullable = false)
    private Long fkCuestionario;

    @Column(name = "puntaje_bruto", nullable = false, precision = 10, scale = 2)
    private BigDecimal puntajeBruto;

    @Column(name = "puntaje_transformado", nullable = false, precision = 10, scale = 2)
    private BigDecimal puntajeTransformado;

    @Column(name = "fk_nivel_riesgo", nullable = false)
    private Long fkNivelRiesgo;

    @Column(name = "fecha_calculo")
    private LocalDateTime fechaCalculo;

}
