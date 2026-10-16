package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Conclusiones y plan de acción del informe de empresa de un cliente en un año.
 */
@Entity
@Table(name = "informe_empresa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformeEmpresaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_cliente", nullable = false)
    private Long fkCliente;

    @Column(nullable = false)
    private Integer anio;

    @Column(columnDefinition = "TEXT")
    private String conclusiones;

    @Column(name = "acciones_corto_plazo", columnDefinition = "TEXT")
    private String accionesCortoPlazo;

    @Column(name = "acciones_mediano_plazo", columnDefinition = "TEXT")
    private String accionesMedianoPlazo;

    @Column(name = "acciones_largo_plazo", columnDefinition = "TEXT")
    private String accionesLargoPlazo;

    @Column(name = "fk_evaluador")
    private Long fkEvaluador;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
