package com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "escala_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_escala", nullable = false)
    private Long fkEscala;

    @Column(name = "fk_opcion_respuesta", nullable = false)
    private Long fkOpcionRespuesta;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

}
