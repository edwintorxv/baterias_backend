package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Enlace para definir la contraseña (V23). Solo se guarda el SHA-256 del código: el código en claro va
 * únicamente en el correo. Vence a las 48 h y es de un solo uso.
 */
@Entity
@Table(name = "enlace_contrasena")
@Data
@NoArgsConstructor
public class EnlaceContrasenaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_usuario", nullable = false)
    private Long fkUsuario;

    @ToString.Exclude
    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropositoEnlace proposito;

    @Column(nullable = false)
    private LocalDateTime vence;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    @Column(nullable = false)
    private Boolean anulado = false;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
