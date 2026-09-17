package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fk_industria", nullable = false)
    private Long fkIndustria;

    @Column(name = "fk_ciudad_municipio")
    private Long fkCiudadMunicipio;

    @Column(name = "nit", nullable = false, length = 50)
    private String nit;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "nombre_contacto", length = 200)
    private String nombreContacto;

    @Column(name = "cargo_contacto", length = 150)
    private String cargoContacto;

    @Column(name = "telefono_contacto", length = 50)
    private String telefonoContacto;

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;


}
