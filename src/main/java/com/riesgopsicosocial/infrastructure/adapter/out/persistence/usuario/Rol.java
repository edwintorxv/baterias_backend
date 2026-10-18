package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

/**
 * Roles de la API (columna {@code usuario.rol}). Cada uno se liga solo a lo suyo (ck_usuario_vinculo).
 */
public enum Rol {

    /** Usuarios, asignaciones y configuración; no ve resultados ni informes. */
    ADMIN,
    /** Trabaja sobre sus clientes asignados; ligado a su evaluador (datos y firma de los informes). */
    PSICOLOGO,
    /** Ve el informe de su empresa. */
    CLIENTE,
    /** Previsto para cuando el trabajador responda en línea; por ahora sin permisos. */
    EVALUADO

}
