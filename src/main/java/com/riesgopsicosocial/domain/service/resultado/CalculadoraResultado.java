package com.riesgopsicosocial.domain.service.resultado;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula el resultado de un cuestionario a partir de sus respuestas, siguiendo el método
 * definido en {@link MetodoCalculo}. No depende de Spring ni de la persistencia.
 *
 * <p>Reglas comunes:
 * <ul>
 *   <li>Puntaje transformado = bruto / factor × 100, redondeado a un decimal (como los baremos).</li>
 *   <li>Cada dimensión debe tener respondidas todas sus preguntas, salvo las
 *       {@code maxItemsSinRespuesta} que admite el manual; si no, no se calcula nada.</li>
 *   <li>Una dimensión con condición cuya pregunta filtro la aplicación tiene en "no" vale
 *       puntaje bruto 0 y no debe traer respuestas.</li>
 *   <li>Si un puntaje no cae en ningún rango del baremo se lanza {@link ReglaNegocioException}:
 *       nunca se devuelven resultados parciales.</li>
 * </ul>
 */
public class CalculadoraResultado {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    public ResultadoCuestionario calcular(ConfiguracionCuestionario config, List<RespuestaCalculo> respuestas,
                                          FiltrosAplicacion filtros) {
        if (config.metodoCalculo() == MetodoCalculo.PROMEDIO_PONDERADO) {
            validarSinItemsOpcionales(config);
        }
        Map<Long, List<BigDecimal>> valoresPorDimension = agruparYValidar(config, respuestas, filtros);

        return switch (config.metodoCalculo()) {
            case SUMA_POR_DOMINIOS -> calcularSumaPorDominios(config, valoresPorDimension);
            case SUMA_DIRECTA -> calcularSumaDirecta(config, valoresPorDimension);
            case PROMEDIO_PONDERADO -> calcularPromedioPonderado(config, valoresPorDimension);
        };
    }

    /**
     * Total general = bruto intralaboral + bruto extralaboral, transformado con la suma de
     * los factores de ambos cuestionarios (616 para A + C, 512 para B + C).
     *
     * @param baremos baremo del total general para la forma intralaboral (no depende del grupo ocupacional)
     */
    public ResultadoTotalGeneral calcularTotalGeneral(ConfiguracionCuestionario configIntralaboral,
                                                      ResultadoCuestionario intralaboral,
                                                      ConfiguracionCuestionario configExtralaboral,
                                                      ResultadoCuestionario extralaboral,
                                                      List<RangoBaremo> baremos) {
        if (configIntralaboral.metodoCalculo() != MetodoCalculo.SUMA_POR_DOMINIOS
                || configExtralaboral.metodoCalculo() != MetodoCalculo.SUMA_DIRECTA) {
            throw new ReglaNegocioException("El total general requiere un cuestionario intralaboral ("
                    + MetodoCalculo.SUMA_POR_DOMINIOS + ") y uno extralaboral (" + MetodoCalculo.SUMA_DIRECTA
                    + "); se recibieron " + configIntralaboral.forma() + " y " + configExtralaboral.forma());
        }

        BigDecimal bruto = intralaboral.puntajeBruto().add(extralaboral.puntajeBruto());
        BigDecimal factor = configIntralaboral.factorTransformacion().add(configExtralaboral.factorTransformacion());
        BigDecimal transformado = transformar(bruto, factor);
        NivelRiesgo nivel = buscarNivel(baremos, transformado,
                "total general (" + configIntralaboral.forma() + " + " + configExtralaboral.forma() + ")",
                "forma intralaboral " + configIntralaboral.forma());
        return new ResultadoTotalGeneral(configIntralaboral.idCuestionario(), configIntralaboral.forma(),
                escalaBruto(bruto), transformado, nivel);
    }

    // ------------------------------------------------------------------
    // Métodos de cálculo
    // ------------------------------------------------------------------

    /** Formas A y B: dimensión → dominio → total. */
    private ResultadoCuestionario calcularSumaPorDominios(ConfiguracionCuestionario config,
                                                          Map<Long, List<BigDecimal>> valores) {
        List<Long> sinDominio = config.dimensiones().stream()
                .filter(d -> config.dominios().stream()
                        .noneMatch(dom -> dom.idDominioCuestionario().equals(d.idDominioCuestionario())))
                .map(ConfiguracionDimension::idDimensionCuestionario)
                .toList();
        if (!sinDominio.isEmpty()) {
            throw new ReglaNegocioException("Configuración incompleta del cuestionario " + config.forma()
                    + ": las dimensiones (dimension_cuestionario) " + sinDominio
                    + " no tienen dominio_cuestionario asociado");
        }

        List<ResultadoDimension> dimensiones = calcularDimensiones(config, valores);

        Map<Long, BigDecimal> brutoPorDimension = dimensiones.stream()
                .collect(Collectors.toMap(ResultadoDimension::idDimensionCuestionario, ResultadoDimension::puntajeBruto));

        List<ResultadoDominio> dominios = new ArrayList<>();
        for (ConfiguracionDominio dominio : config.dominios()) {
            List<ConfiguracionDimension> suyas = config.dimensiones().stream()
                    .filter(d -> dominio.idDominioCuestionario().equals(d.idDominioCuestionario()))
                    .toList();
            if (suyas.isEmpty()) {
                continue;
            }
            BigDecimal bruto = suyas.stream()
                    .map(d -> brutoPorDimension.get(d.idDimensionCuestionario()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal transformado = transformar(bruto, dominio.factorTransformacion());
            NivelRiesgo nivel = buscarNivel(dominio.baremos(), transformado,
                    "dominio '" + dominio.nombre() + "' del cuestionario " + config.forma(), config);
            dominios.add(new ResultadoDominio(dominio.idDominioCuestionario(), dominio.nombre(),
                    escalaBruto(bruto), transformado, nivel));
        }

        BigDecimal brutoTotal = sumar(brutoPorDimension.values());
        return totalizar(config, brutoTotal, dominios, dimensiones);
    }

    /** Forma C (extralaboral): dimensión → total. */
    private ResultadoCuestionario calcularSumaDirecta(ConfiguracionCuestionario config,
                                                      Map<Long, List<BigDecimal>> valores) {
        List<ResultadoDimension> dimensiones = calcularDimensiones(config, valores);
        BigDecimal brutoTotal = sumar(dimensiones.stream().map(ResultadoDimension::puntajeBruto).toList());
        return totalizar(config, brutoTotal, List.of(), dimensiones);
    }

    /**
     * Forma D (estrés): total bruto = Σ promedio(ítems de la dimensión) × peso.
     * No hay baremos por dimensión, así que solo se devuelve el total.
     */
    private ResultadoCuestionario calcularPromedioPonderado(ConfiguracionCuestionario config,
                                                            Map<Long, List<BigDecimal>> valores) {
        BigDecimal brutoTotal = BigDecimal.ZERO;
        for (ConfiguracionDimension dimension : config.dimensiones()) {
            if (dimension.peso() == null) {
                throw new ReglaNegocioException("Configuración incompleta del cuestionario " + config.forma()
                        + ": la dimensión '" + dimension.nombre() + "' no tiene peso definido");
            }
            List<BigDecimal> items = valores.get(dimension.idDimensionCuestionario());
            BigDecimal promedio = sumar(items).divide(BigDecimal.valueOf(items.size()), PRECISION);
            brutoTotal = brutoTotal.add(promedio.multiply(dimension.peso()));
        }
        return totalizar(config, brutoTotal, List.of(), List.of());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private List<ResultadoDimension> calcularDimensiones(ConfiguracionCuestionario config,
                                                         Map<Long, List<BigDecimal>> valores) {
        List<ResultadoDimension> resultados = new ArrayList<>();
        for (ConfiguracionDimension dimension : config.dimensiones()) {
            BigDecimal bruto = sumar(valores.get(dimension.idDimensionCuestionario()));
            BigDecimal transformado = transformar(bruto, dimension.factorTransformacion());
            NivelRiesgo nivel = buscarNivel(dimension.baremos(), transformado,
                    "dimensión '" + dimension.nombre() + "' del cuestionario " + config.forma(), config);
            resultados.add(new ResultadoDimension(dimension.idDimensionCuestionario(), dimension.nombre(),
                    escalaBruto(bruto), transformado, nivel));
        }
        return resultados;
    }

    private ResultadoCuestionario totalizar(ConfiguracionCuestionario config, BigDecimal brutoTotal,
                                            List<ResultadoDominio> dominios, List<ResultadoDimension> dimensiones) {
        BigDecimal transformado = transformar(brutoTotal, config.factorTransformacion());
        NivelRiesgo nivel = buscarNivel(config.baremos(), transformado,
                "total del cuestionario " + config.forma(), config);
        return new ResultadoCuestionario(config.idCuestionario(), config.forma(),
                escalaBruto(brutoTotal), transformado, nivel, dominios, dimensiones);
    }

    /**
     * El promedio de PROMEDIO_PONDERADO divide por los ítems respondidos: una dimensión que
     * pudiera quedar vacía o incompleta haría el cálculo inválido.
     */
    private void validarSinItemsOpcionales(ConfiguracionCuestionario config) {
        List<String> invalidas = config.dimensiones().stream()
                .filter(d -> d.condicion() != null || d.maxItemsSinRespuesta() > 0)
                .map(d -> "'" + d.nombre() + "'")
                .toList();
        if (!invalidas.isEmpty()) {
            throw new ReglaNegocioException("Configuración inválida del cuestionario " + config.forma()
                    + ": las dimensiones " + String.join(", ", invalidas)
                    + " tienen condición o ítems sin respuesta permitidos, que el método "
                    + MetodoCalculo.PROMEDIO_PONDERADO + " no admite");
        }
    }

    /**
     * Agrupa los valores por dimensión y valida que cada una tenga sus preguntas respondidas
     * (con la tolerancia de la dimensión), o ninguna si la pregunta filtro la deja sin aplicar.
     */
    private Map<Long, List<BigDecimal>> agruparYValidar(ConfiguracionCuestionario config,
                                                        List<RespuestaCalculo> respuestas,
                                                        FiltrosAplicacion filtros) {
        if (config.dimensiones().isEmpty()) {
            throw new ReglaNegocioException("El cuestionario " + config.forma() + " no tiene dimensiones configuradas");
        }

        Set<Long> preguntasVistas = new HashSet<>();
        List<Long> duplicadas = new ArrayList<>();
        Map<Long, List<BigDecimal>> valores = new HashMap<>();
        for (RespuestaCalculo r : respuestas) {
            if (!preguntasVistas.add(r.idPregunta())) {
                duplicadas.add(r.idPregunta());
            }
            valores.computeIfAbsent(r.idDimensionCuestionario(), k -> new ArrayList<>()).add(r.valor());
        }
        if (!duplicadas.isEmpty()) {
            throw new ReglaNegocioException("El cuestionario " + config.forma()
                    + " tiene preguntas respondidas más de una vez: " + duplicadas);
        }

        List<String> problemas = new ArrayList<>();
        for (ConfiguracionDimension dimension : config.dimensiones()) {
            // Una dimensión que no aplica queda con lista vacía → puntaje bruto 0.
            int respondidas = valores.computeIfAbsent(dimension.idDimensionCuestionario(), k -> new ArrayList<>()).size();
            if (dimension.condicion() != null && filtros.noAplica(dimension.condicion())) {
                if (respondidas > 0) {
                    problemas.add("'" + dimension.nombre() + "' no aplica (" + dimension.condicion()
                            + " = no) pero tiene " + respondidas + " respuestas");
                }
            } else if (dimension.numeroPreguntas() == 0) {
                problemas.add("'" + dimension.nombre() + "' no tiene preguntas configuradas");
            } else if (dimension.numeroPreguntas() - respondidas > dimension.maxItemsSinRespuesta()) {
                problemas.add("'" + dimension.nombre() + "' tiene " + respondidas + " de "
                        + dimension.numeroPreguntas() + " preguntas respondidas"
                        + (dimension.maxItemsSinRespuesta() > 0
                        ? " (se admite hasta " + dimension.maxItemsSinRespuesta() + " sin respuesta)" : ""));
            }
        }
        if (!problemas.isEmpty()) {
            throw new ReglaNegocioException("No se puede calcular el cuestionario " + config.forma() + ": "
                    + String.join("; ", problemas));
        }
        return valores;
    }

    private NivelRiesgo buscarNivel(List<RangoBaremo> baremos, BigDecimal puntaje, String que,
                                    ConfiguracionCuestionario config) {
        return buscarNivel(baremos, puntaje, que, "grupo ocupacional " + config.idGrupoOcupacional());
    }

    /** @param baremo de qué baremo se trata, para el mensaje de error (ej. "grupo ocupacional 1") */
    private NivelRiesgo buscarNivel(List<RangoBaremo> baremos, BigDecimal puntaje, String que, String baremo) {
        if (baremos.isEmpty()) {
            throw new ReglaNegocioException("No hay baremo para " + que + " y " + baremo);
        }
        return baremos.stream()
                .filter(rango -> rango.contiene(puntaje))
                .findFirst()
                .map(RangoBaremo::nivelRiesgo)
                .orElseThrow(() -> new ReglaNegocioException("El puntaje transformado " + puntaje + " de " + que
                        + " no cae en ningún rango del baremo del " + baremo));
    }

    private BigDecimal transformar(BigDecimal bruto, BigDecimal factor) {
        return bruto.multiply(CIEN).divide(factor, PRECISION).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal escalaBruto(BigDecimal bruto) {
        return bruto.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(Collection<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
