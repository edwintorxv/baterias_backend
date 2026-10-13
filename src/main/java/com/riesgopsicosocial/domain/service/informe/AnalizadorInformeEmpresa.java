package com.riesgopsicosocial.domain.service.informe;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDimension;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDominio;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agrega los resultados individuales de las personas de una empresa en las tablas del
 * informe de empresa. No depende de Spring ni de la persistencia.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Cobertura: esperados = personas cargadas; evaluados = las que respondieron alguna prueba.</li>
 *   <li>Cada tabla cuenta solo a quienes tienen resultado de ese cuestionario, y su porcentaje
 *       se calcula sobre ese total (a un decimal, HALF_UP).</li>
 *   <li>Intralaboral, extralaboral y estrés se distinguen por el método de cálculo del
 *       cuestionario (A/B, C y D), no por su letra. Intralaboral junta A y B: cada persona
 *       presenta una sola y su nivel ya sale del baremo de su forma.</li>
 *   <li>Dominios y, dentro de cada uno, sus dimensiones: ranking del más crítico al menos crítico
 *       ({@link DistribucionNiveles#masCriticaPrimero()}); en empate total se respeta el orden del
 *       cuestionario.</li>
 *   <li>Una dimensión condicional (demandas emocionales, relación con los colaboradores) no cuenta
 *       a quien respondió "no" a su pregunta filtro: su puntaje 0 no es "sin riesgo", es que no aplica.</li>
 *   <li>Cobertura por ciudad de trabajo: se agrupa por id de ciudad; sin ciudad = "Sin ciudad".</li>
 *   <li>Áreas: se agrupan sin distinguir mayúsculas, tildes ni espacios repetidos; se muestran
 *       con la escritura más frecuente. Sin área = "Sin área".</li>
 * </ul>
 */
public class AnalizadorInformeEmpresa {

    public static final String SIN_AREA = "Sin área";
    public static final String SIN_CIUDAD = "Sin ciudad";

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final List<CuestionarioInforme> cuestionarios;
    private final Map<Long, CuestionarioInforme> cuestionarioPorId;
    private final List<NivelRiesgo> niveles;

    /**
     * @param cuestionarios estructura de todos los cuestionarios que pueden aparecer en los resultados
     * @param niveles       niveles de riesgo en el orden en que se muestran
     */
    public AnalizadorInformeEmpresa(List<CuestionarioInforme> cuestionarios, List<NivelRiesgo> niveles) {
        this.cuestionarios = List.copyOf(cuestionarios);
        this.cuestionarioPorId = cuestionarios.stream()
                .collect(Collectors.toMap(CuestionarioInforme::idCuestionario, Function.identity()));
        this.niveles = List.copyOf(niveles);
    }

    public AnalisisGrupo analizar(List<ParticipanteEmpresa> participantes) {
        List<ResultadoCuestionario> intralaborales = resultadosPorMetodo(participantes, MetodoCalculo.SUMA_POR_DOMINIOS);
        return new AnalisisGrupo(
                cobertura(participantes),
                coberturaPorCiudad(participantes),
                (int) participantes.stream().filter(ParticipanteEmpresa::pendienteDeCalculo).count(),
                distribuir(intralaborales.stream().map(ResultadoCuestionario::nivelRiesgo).toList()),
                distribuir(nivelesDe(resultadosPorMetodo(participantes, MetodoCalculo.SUMA_DIRECTA))),
                distribuir(nivelesDe(resultadosPorMetodo(participantes, MetodoCalculo.PROMEDIO_PONDERADO))),
                dominiosPorForma(participantes));
    }

    /**
     * Un análisis por área, de la de más personas esperadas a la de menos (empate: por nombre).
     */
    public List<AnalisisArea> analizarPorArea(List<ParticipanteEmpresa> participantes) {
        Map<String, List<ParticipanteEmpresa>> porArea = participantes.stream()
                .collect(Collectors.groupingBy(p -> claveArea(p.area()), LinkedHashMap::new, Collectors.toList()));

        return porArea.values().stream()
                .map(grupo -> new AnalisisArea(nombreArea(grupo), analizar(grupo)))
                .sorted(Comparator.comparingInt((AnalisisArea a) -> a.analisis().cobertura().esperados()).reversed()
                        .thenComparing(AnalisisArea::area))
                .toList();
    }

    private Cobertura cobertura(List<ParticipanteEmpresa> participantes) {
        int esperados = participantes.size();
        int evaluados = (int) participantes.stream().filter(ParticipanteEmpresa::tieneRespuestas).count();
        return new Cobertura(esperados, evaluados, porcentaje(evaluados, esperados));
    }

    private List<CoberturaCiudad> coberturaPorCiudad(List<ParticipanteEmpresa> participantes) {
        Map<Optional<Long>, List<ParticipanteEmpresa>> porCiudad = participantes.stream()
                .collect(Collectors.groupingBy(p -> Optional.ofNullable(p.ciudad()).map(CiudadTrabajo::id),
                        LinkedHashMap::new, Collectors.toList()));
        return porCiudad.entrySet().stream()
                .map(e -> {
                    CiudadTrabajo ciudad = e.getValue().get(0).ciudad();
                    return new CoberturaCiudad(e.getKey().orElse(null),
                            ciudad == null ? SIN_CIUDAD : ciudad.nombre(), cobertura(e.getValue()));
                })
                .sorted(Comparator.comparingInt((CoberturaCiudad c) -> c.cobertura().esperados()).reversed()
                        .thenComparing(CoberturaCiudad::ciudad))
                .toList();
    }

    private List<ResultadoCuestionario> resultadosPorMetodo(List<ParticipanteEmpresa> participantes, MetodoCalculo metodo) {
        return participantes.stream()
                .filter(p -> p.resultado() != null)
                .flatMap(p -> p.resultado().cuestionarios().stream())
                .filter(rc -> cuestionario(rc.idCuestionario()).metodoCalculo() == metodo)
                .toList();
    }

    /** Resultado intralaboral de una persona con sus preguntas filtro. */
    private record Intralaboral(ResultadoCuestionario resultado, FiltrosAplicacion filtros) {

        boolean aplica(DimensionInforme dimension) {
            return dimension.condicion() == null || filtros == null || !filtros.noAplica(dimension.condicion());
        }
    }

    private List<DominiosForma> dominiosPorForma(List<ParticipanteEmpresa> participantes) {
        Map<Long, List<Intralaboral>> porCuestionario = participantes.stream()
                .filter(p -> p.resultado() != null)
                .flatMap(p -> p.resultado().cuestionarios().stream()
                        .filter(rc -> cuestionario(rc.idCuestionario()).metodoCalculo() == MetodoCalculo.SUMA_POR_DOMINIOS)
                        .map(rc -> new Intralaboral(rc, p.filtros())))
                .collect(Collectors.groupingBy(i -> i.resultado().idCuestionario()));

        return cuestionarios.stream()
                .filter(c -> porCuestionario.containsKey(c.idCuestionario()))
                .map(c -> {
                    List<Intralaboral> deLaForma = porCuestionario.get(c.idCuestionario());
                    List<DistribucionDominio> dominios = c.dominios().stream()
                            .map(d -> distribucionDominio(d, deLaForma))
                            .sorted(Comparator.comparing(DistribucionDominio::distribucion,
                                    DistribucionNiveles.masCriticaPrimero()))
                            .toList();
                    return new DominiosForma(c.idCuestionario(), c.forma(), deLaForma.size(), dominios);
                })
                .toList();
    }

    private DistribucionDominio distribucionDominio(DominioInforme dominio, List<Intralaboral> deLaForma) {
        List<NivelRiesgo> nivelesObtenidos = deLaForma.stream()
                .flatMap(i -> i.resultado().dominios().stream())
                .filter(rd -> rd.idDominioCuestionario().equals(dominio.idDominioCuestionario()))
                .map(ResultadoDominio::nivelRiesgo)
                .toList();
        List<DistribucionDimension> dimensiones = dominio.dimensiones().stream()
                .map(d -> distribucionDimension(d, deLaForma))
                .sorted(Comparator.comparing(DistribucionDimension::distribucion, DistribucionNiveles.masCriticaPrimero()))
                .toList();
        return new DistribucionDominio(dominio.idDominioCuestionario(), dominio.nombre(), distribuir(nivelesObtenidos),
                dimensiones);
    }

    private DistribucionDimension distribucionDimension(DimensionInforme dimension, List<Intralaboral> deLaForma) {
        List<NivelRiesgo> nivelesObtenidos = deLaForma.stream()
                .filter(i -> i.aplica(dimension))
                .flatMap(i -> i.resultado().dimensiones().stream())
                .filter(rd -> rd.idDimensionCuestionario().equals(dimension.idDimensionCuestionario()))
                .map(ResultadoDimension::nivelRiesgo)
                .toList();
        return new DistribucionDimension(dimension.idDimensionCuestionario(), dimension.nombre(),
                distribuir(nivelesObtenidos));
    }

    private static List<NivelRiesgo> nivelesDe(List<ResultadoCuestionario> resultados) {
        return resultados.stream().map(ResultadoCuestionario::nivelRiesgo).toList();
    }

    private DistribucionNiveles distribuir(List<NivelRiesgo> nivelesObtenidos) {
        Map<Long, Long> cantidadPorNivel = nivelesObtenidos.stream()
                .collect(Collectors.groupingBy(NivelRiesgo::id, Collectors.counting()));
        int total = nivelesObtenidos.size();
        List<ConteoNivel> conteos = niveles.stream()
                .map(n -> {
                    int cantidad = cantidadPorNivel.getOrDefault(n.id(), 0L).intValue();
                    return new ConteoNivel(n, cantidad, porcentaje(cantidad, total));
                })
                .toList();
        return new DistribucionNiveles(total, conteos);
    }

    private CuestionarioInforme cuestionario(Long idCuestionario) {
        CuestionarioInforme cuestionario = cuestionarioPorId.get(idCuestionario);
        if (cuestionario == null) {
            throw new ReglaNegocioException("No se encontró la configuración del cuestionario " + idCuestionario
                    + " para el informe de empresa");
        }
        return cuestionario;
    }

    private static BigDecimal porcentaje(int parte, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(parte).multiply(CIEN)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    static String claveArea(String area) {
        if (area == null || area.isBlank()) {
            return "";
        }
        String sinTildes = Normalizer.normalize(area.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** La escritura más frecuente del grupo; en empate, la primera en orden alfabético. */
    private static String nombreArea(List<ParticipanteEmpresa> grupo) {
        return grupo.stream()
                .map(ParticipanteEmpresa::area)
                .filter(a -> a != null && !a.isBlank())
                .map(a -> a.trim().replaceAll("\\s+", " "))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(SIN_AREA);
    }

}
