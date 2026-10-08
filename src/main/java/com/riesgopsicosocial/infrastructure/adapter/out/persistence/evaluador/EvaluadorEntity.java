package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluador")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluadorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_identificacion", nullable = false, length = 30)
    private String numeroIdentificacion;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String profesion;

    @Column(length = 200)
    private String posgrado;

    @Column(name = "tarjeta_profesional", nullable = false, length = 50)
    private String tarjetaProfesional;

    @Column(name = "licencia_salud_ocupacional", nullable = false, length = 50)
    private String licenciaSaludOcupacional;

    @Column(name = "fecha_expedicion_licencia", nullable = false)
    private LocalDate fechaExpedicionLicencia;

    /** Imagen de la firma (BYTEA); se gestiona aparte con PUT/GET /evaluadores/{id}/firma. */
    @Column(name = "firma")
    private byte[] firma;

    @Column(name = "firma_tipo_contenido", length = 50)
    private String firmaTipoContenido;

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
