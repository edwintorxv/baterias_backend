package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "aplicacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AplicacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_evaluado_cliente", nullable = false)
    private Long fkEvaluadoCliente;

    @Column(name = "fk_grupo_ocupacional", nullable = false)
    private Long fkGrupoOcupacional;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDateTime fechaAplicacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(length = 20)
    private String estado;

    @Column(name = "atiende_clientes")
    private Boolean atiendeClientes;

    @Column(name = "es_jefe")
    private Boolean esJefe;

    @Column(name = "fk_evaluador")
    private Long fkEvaluador;

    @Column(columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
