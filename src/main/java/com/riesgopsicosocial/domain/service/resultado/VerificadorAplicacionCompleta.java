package com.riesgopsicosocial.domain.service.resultado;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.resultado.configuracion.ConfiguracionCuestionario;
import com.riesgopsicosocial.domain.model.resultado.configuracion.ConfiguracionDimension;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RespuestaCalculo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Solo se calcula una aplicación completa: todas las preguntas que aplican de todos los cuestionarios
 * de su grupo ocupacional. Es más estricto que el manual (que admite ítems sin respuesta en algunas
 * dimensiones, ver {@link CalculadoraResultado}) a propósito: un cálculo con faltantes podría tomarse
 * como finalizado y quedarse así (decisión del usuario, para evitar el error humano).
 */
public final class VerificadorAplicacionCompleta {

    private VerificadorAplicacionCompleta() {
    }

    /** @throws ReglaNegocioException con lo que falta por cuestionario, en el orden recibido */
    public static void exigirCompleta(Collection<ConfiguracionCuestionario> cuestionarios,
                                      List<RespuestaCalculo> respuestas, FiltrosAplicacion filtros) {
        Map<Long, Set<Long>> respondidasPorDimension = respuestas.stream()
                .collect(Collectors.groupingBy(RespuestaCalculo::idDimensionCuestionario,
                        Collectors.mapping(RespuestaCalculo::idPregunta, Collectors.toSet())));

        List<String> faltantes = new ArrayList<>();
        for (ConfiguracionCuestionario cuestionario : cuestionarios) {
            int faltan = 0;
            for (ConfiguracionDimension dimension : cuestionario.dimensiones()) {
                if (dimension.condicion() != null && filtros.noAplica(dimension.condicion())) {
                    continue;
                }
                int respondidas = respondidasPorDimension.getOrDefault(dimension.idDimensionCuestionario(), Set.of()).size();
                faltan += Math.max(0, dimension.numeroPreguntas() - respondidas);
            }
            if (faltan > 0) {
                faltantes.add(faltan + (faltan == 1 ? " pregunta" : " preguntas") + " del cuestionario " + cuestionario.forma());
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("La aplicación está incompleta: falta"
                    + (faltantes.size() == 1 && faltantes.get(0).startsWith("1 ") ? " " : "n ")
                    + unir(faltantes) + ". Complete todas las preguntas antes de calcular");
        }
    }

    private static String unir(List<String> partes) {
        if (partes.size() == 1) {
            return partes.get(0);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
    }

}
