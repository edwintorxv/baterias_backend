package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Una prueba desglosada por ciudad o por área, protegiendo los grupos pequeños: los que no llegan
 * al mínimo de personas se juntan en "Otras ciudades" / "Otras áreas"; si aun juntos no llegan,
 * se omiten.
 *
 * @param grupos    de el de más personas al de menos (empate: por nombre); "Otras…", si existe, al final
 * @param agrupados grupos pequeños que se juntaron en "Otras…" (0 si no hay)
 * @param omitidos  personas de grupos pequeños que no se muestran porque, aun juntas, no llegan al mínimo
 */
public record DesgloseGrupos(List<DistribucionGrupo> grupos, int agrupados, int omitidos) {
}
