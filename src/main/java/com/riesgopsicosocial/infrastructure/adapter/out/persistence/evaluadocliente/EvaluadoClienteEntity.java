package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluado_cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluadoClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_evaluado", nullable = false)
    private Long fkEvaluado;

    @Column(name = "fk_cliente", nullable = false)
    private Long fkCliente;

    @Column(name = "fk_ciudad_trabajo")
    private Long fkCiudadTrabajo;

    @Column(name = "antiguedad_empresa", length = 100)
    private String antiguedadEmpresa;

    @Column(name = "nombre_cargo", length = 200)
    private String nombreCargo;

    @Column(name = "fk_tipo_cargo")
    private Long fkTipoCargo;

    @Column(name = "fk_tiempo_cargo")
    private Long fkTiempoCargo;

    @Column(name = "nombre_area", length = 200)
    private String nombreArea;

    @Column(name = "fk_tipo_contrato")
    private Long fkTipoContrato;

    @Column(name = "fk_horas_labor")
    private Long fkHorasLabor;

    @Column(name = "fk_tipo_salario")
    private Long fkTipoSalario;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
