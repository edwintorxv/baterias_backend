package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Distribución por nivel de un grupo (ciudad de trabajo o área). El {@code total} de cada
 * distribución es la cantidad de personas del grupo con resultado de esa prueba.
 *
 * @param intralaboral total intralaboral (A y B juntas)
 * @param estres       síntomas de estrés (D)
 */
public record DistribucionGrupo(String grupo, DistribucionNiveles intralaboral, DistribucionNiveles estres) {

    /** Personas del grupo con alguna de las dos pruebas: ordena los grupos. */
    public int personas() {
        return Math.max(intralaboral.total(), estres.total());
    }

}
