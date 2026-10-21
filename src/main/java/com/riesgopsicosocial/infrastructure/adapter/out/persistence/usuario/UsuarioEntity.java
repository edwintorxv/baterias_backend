package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Único sin distinguir mayúsculas (índice sobre LOWER). */
    /** Con él se ingresa: en minúsculas y único (V20). */
    @Column(nullable = false, length = 100)
    private String correo;

    /** BCrypt; nunca sale en las respuestas. */
    @ToString.Exclude
    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Solo PSICOLOGO (obligatorio). */
    @Column(name = "fk_evaluador")
    private Long fkEvaluador;

    /** Solo CLIENTE (obligatorio). */
    @Column(name = "fk_cliente")
    private Long fkCliente;

    /** Solo EVALUADO (obligatorio). */
    @Column(name = "fk_evaluado")
    private Long fkEvaluado;

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
