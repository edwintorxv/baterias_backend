package com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pregunta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_dimension_cuestionario", nullable = false)
    private Long fkDimensionCuestionario;

    @Column(name = "fk_escala", nullable = false)
    private Long fkEscala;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

}
