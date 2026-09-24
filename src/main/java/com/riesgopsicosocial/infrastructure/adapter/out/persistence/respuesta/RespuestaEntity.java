package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "respuesta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_aplicacion", nullable = false)
    private Long fkAplicacion;

    @Column(name = "fk_pregunta", nullable = false)
    private Long fkPregunta;

    @Column(name = "fk_opcion_respuesta", nullable = false)
    private Long fkOpcionRespuesta;

    @Column(name = "valor_obtenido", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorObtenido;

    @Column(name = "fecha_respuesta", insertable = false, updatable = false)
    private LocalDateTime fechaRespuesta;

}
