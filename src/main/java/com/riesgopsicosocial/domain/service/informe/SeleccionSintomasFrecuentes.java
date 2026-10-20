package com.riesgopsicosocial.domain.service.informe;

import com.riesgopsicosocial.domain.model.informe.GrupoSintomas;
import com.riesgopsicosocial.domain.model.informe.SintomaFrecuente;
import com.riesgopsicosocial.domain.model.informe.empresa.DimensionInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ItemSintoma;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Síntomas de estrés que un trabajador reportó con frecuencia en una aplicación, agrupados por dimensión
 * para el informe individual. Frecuente = una de las dos primeras opciones (Siempre, Casi siempre).
 */
public final class SeleccionSintomasFrecuentes {

    /** Cuántas opciones, desde la más frecuente, cuentan como "con frecuencia". */
    static final int OPCIONES_FRECUENTES = 2;

    private SeleccionSintomasFrecuentes() {
    }

    /**
     * @param dimensiones del cuestionario de estrés, en su orden (fisiológicos primero)
     * @param items       ítems del cuestionario de estrés, con su texto
     * @param respuestas  respuestas de la aplicación a los ítems de estrés
     * @param opciones    opciones de estrés, de la más frecuente (Siempre) a la menos (Nunca)
     * @return una entrada por dimensión, aunque no tenga síntomas frecuentes; vacía si la aplicación no
     *         tiene respuestas de estrés
     */
    public static List<GrupoSintomas> seleccionar(List<DimensionInforme> dimensiones, List<ItemSintoma> items,
                                                  List<RespuestaSintoma> respuestas, List<OpcionFrecuencia> opciones) {
        if (respuestas.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> posicion = opciones.stream()
                .limit(OPCIONES_FRECUENTES)
                .collect(Collectors.toMap(OpcionFrecuencia::id, opciones::indexOf));
        Map<Long, String> nombreOpcion = opciones.stream()
                .collect(Collectors.toMap(OpcionFrecuencia::id, OpcionFrecuencia::nombre));
        Map<Long, ItemSintoma> itemPorPregunta = items.stream()
                .collect(Collectors.toMap(ItemSintoma::idPregunta, Function.identity()));

        Map<Long, List<RespuestaSintoma>> frecuentesPorDimension = respuestas.stream()
                .filter(r -> posicion.containsKey(r.idOpcion()) && itemPorPregunta.containsKey(r.idPregunta()))
                .sorted(Comparator.comparing((RespuestaSintoma r) -> posicion.get(r.idOpcion()))
                        .thenComparingInt(r -> itemPorPregunta.get(r.idPregunta()).numero()))
                .collect(Collectors.groupingBy(RespuestaSintoma::idDimensionCuestionario));

        return dimensiones.stream()
                .map(d -> new GrupoSintomas(d.nombre(),
                        frecuentesPorDimension.getOrDefault(d.idDimensionCuestionario(), List.of()).stream()
                                .map(r -> {
                                    ItemSintoma item = itemPorPregunta.get(r.idPregunta());
                                    return new SintomaFrecuente(item.numero(), item.descripcion(),
                                            nombreOpcion.get(r.idOpcion()));
                                })
                                .toList()))
                .toList();
    }

}
