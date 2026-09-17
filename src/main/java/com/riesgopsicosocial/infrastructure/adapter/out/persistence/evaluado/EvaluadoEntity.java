package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_identificacion", nullable = false, length = 30)
    private String numeroIdentificacion;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String apellido;

    @Column(name = "fk_sexo")
    private Long fkSexo;

    @Column(name = "anio_nacimiento")
    private Integer anioNacimiento;

    @Column(name = "fk_estado_civil")
    private Long fkEstadoCivil;

    @Column(name = "fk_nivel_academico")
    private Long fkNivelAcademico;

    @Column(name = "fk_ciudad_residencia")
    private Long fkCiudadResidencia;

    @Column(name = "fk_estrato_socioeconomico")
    private Long fkEstratoSocioeconomico;

    @Column(name = "fk_tipo_vivienda")
    private Long fkTipoVivienda;

    @Column(name = "ocupacion_profesion", length = 200)
    private String ocupacionProfesion;

    @Column(name = "familiares_dependientes_economicamente")
    private Integer familiaresDependientesEconomicamente;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
