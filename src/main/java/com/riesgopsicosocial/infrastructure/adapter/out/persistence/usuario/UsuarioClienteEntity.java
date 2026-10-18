package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Cliente asignado a un psicólogo. */
@Entity
@Table(name = "usuario_cliente")
@IdClass(UsuarioClienteEntity.Id.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioClienteEntity {

    @jakarta.persistence.Id
    @Column(name = "fk_usuario")
    private Long fkUsuario;

    @jakarta.persistence.Id
    @Column(name = "fk_cliente")
    private Long fkCliente;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    public UsuarioClienteEntity(Long fkUsuario, Long fkCliente) {
        this.fkUsuario = fkUsuario;
        this.fkCliente = fkCliente;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {
        private Long fkUsuario;
        private Long fkCliente;
    }

}
