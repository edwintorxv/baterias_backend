package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * La persona: solo los datos que no cambian. Los que cambian con el tiempo (estado civil, nivel académico,
 * residencia, estrato, vivienda, ocupación, dependientes) están en {@code evaluado_cliente}, uno por
 * vinculación (V17). Las columnas viejas siguen en la tabla, sin uso, hasta que se borren.
 */
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

    @Column(name = "fecha_creado", insertable = false, updatable = false)
    private LocalDateTime fechaCreado;

    @Column(name = "fecha_modificado")
    private LocalDateTime fechaModificado;

    @Column(name = "usuario_crea", length = 100)
    private String usuarioCrea;

    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

}
