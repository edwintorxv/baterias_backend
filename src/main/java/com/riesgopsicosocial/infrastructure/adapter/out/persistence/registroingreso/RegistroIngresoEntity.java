package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un intento de inicio de sesión (V22). Nunca guarda la contraseña. */
@Entity
@Table(name = "registro_ingreso")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroIngresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha;

    /** El que se escribió, normalizado, aunque no exista. */
    @Column(nullable = false, length = 100)
    private String correo;

    @Column(name = "fk_usuario")
    private Long fkUsuario;

    @Column(nullable = false, length = 45)
    private String ip;

    /** User-Agent recortado a 255 caracteres. */
    @Column(length = 255)
    private String navegador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoIngreso resultado;

}
