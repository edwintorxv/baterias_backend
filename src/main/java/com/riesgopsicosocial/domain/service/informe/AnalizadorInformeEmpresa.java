package com.riesgopsicosocial.domain.service.informe;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDimension;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDominio;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RangoBaremo;
import com.riesgopsicosocial.domain.service.resultado.CalculadoraResultado;

import java.math.BigDecimal;
import java.math.MathContext;
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
 *   <li>Concentración del riesgo: dominios con A y B juntas, cada uno identificado por su dominio
 *       común; por ciudad y por área, la distribución por nivel del total intralaboral y del estrés.
 *       Los grupos con menos de {@link #MINIMO_PERSONAS_GRUPO} personas se juntan en "Otras ciudades" /
 *       "Otras áreas" (o se omiten si aun juntos no llegan), para no exponer resultados individuales.</li>
 * </ul>
 */
public class AnalizadorInformeEmpresa {

    public static final String SIN_AREA = "Sin área";
    public static final String SIN_CIUDAD = "Sin ciudad";
    public static final String OTRAS_CIUDADES = "Otras ciudades";
    public static final String OTRAS_AREAS = "Otras áreas";
    /**
     * Confidencialidad: un grupo (ciudad o área) con menos personas no se muestra solo, porque su
     * resultado sería el de una persona identificable. Valor provisional del usuario (2026-10-15).
     */
    public static final int MINIMO_PERSONAS_GRUPO = 2;

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
                distribuir(participantes.stream()
                        .filter(p -> p.resultado() != null && p.resultado().totalGeneral() != null)
                        .map(p -> p.resultado().totalGeneral().nivelRiesgo())
                        .toList()),
                distribuir(nivelesDe(resultadosPorMetodo(participantes, MetodoCalculo.PROMEDIO_PONDERADO))),
                dominiosPorForma(participantes));
    }

    /**
     * Un análisis por área, de la de más personas esperadas a la de menos (empate: por nombre).
     */
    public List<AnalisisArea> analizarPorArea(List<ParticipanteEmpresa> participantes) {
        return porArea(participantes).stream()
                .map(grupo -> new AnalisisArea(nombreArea(grupo), analizar(grupo)))
                .sorted(Comparator.comparingInt((AnalisisArea a) -> a.analisis().cobertura().esperados()).reversed()
                        .thenComparing(AnalisisArea::area))
                .toList();
    }

    /**
     * Concentración del riesgo: distribución por nivel de cada dominio intralaboral con A y B juntas
     * (ranking del más crítico al menos crítico) y, por ciudad de trabajo y por área, del total
     * intralaboral y del estrés, cada uno con su propio desglose ({@link #desglose}). Solo aparecen
     * los dominios de las formas presentadas.
     */
    public ConcentracionRiesgo concentracionRiesgo(List<ParticipanteEmpresa> participantes) {
        Map<Long, Long> dominioPorDominioCuestionario = new HashMap<>();
        Map<Long, String> nombresDominio = new LinkedHashMap<>();
        Set<Long> formasPresentadas = resultadosPorMetodo(participantes, MetodoCalculo.SUMA_POR_DOMINIOS).stream()
                .map(ResultadoCuestionario::idCuestionario)
                .collect(Collectors.toSet());
        cuestionarios.stream()
                .filter(c -> formasPresentadas.contains(c.idCuestionario()))
                .flatMap(c -> c.dominios().stream())
                .forEach(d -> {
                    dominioPorDominioCuestionario.put(d.idDominioCuestionario(), d.idDominio());
                    nombresDominio.putIfAbsent(d.idDominio(), d.nombre());
                });

        List<DominioConsolidado> dominios = nombresDominio.entrySet().stream()
                .map(e -> new DominioConsolidado(e.getKey(), e.getValue(), distribuir(
                        nivelesDominio(participantes, e.getKey(), dominioPorDominioCuestionario))))
                .sorted(Comparator.comparing(DominioConsolidado::distribucion, DistribucionNiveles.masCriticaPrimero()))
                .toList();

        Collection<List<ParticipanteEmpresa>> porCiudad = porCiudad(participantes);
        Collection<List<ParticipanteEmpresa>> porArea = porArea(participantes);

        return new ConcentracionRiesgo(dominios,
                desglose(porCiudad, AnalizadorInformeEmpresa::nombreCiudad, MetodoCalculo.SUMA_POR_DOMINIOS, OTRAS_CIUDADES),
                desglose(porArea, AnalizadorInformeEmpresa::nombreArea, MetodoCalculo.SUMA_POR_DOMINIOS, OTRAS_AREAS),
                desglose(porCiudad, AnalizadorInformeEmpresa::nombreCiudad, MetodoCalculo.PROMEDIO_PONDERADO, OTRAS_CIUDADES),
                desglose(porArea, AnalizadorInformeEmpresa::nombreArea, MetodoCalculo.PROMEDIO_PONDERADO, OTRAS_AREAS));
    }

    /**
     * Factores extralaborales: ranking de las dimensiones del cuestionario extralaboral y nivel total
     * por ciudad de trabajo y por área (con los grupos pequeños protegidos).
     */
    public ResultadosExtralaborales extralaboral(List<ParticipanteEmpresa> participantes) {
        List<ResultadoCuestionario> resultados = resultadosPorMetodo(participantes, MetodoCalculo.SUMA_DIRECTA);
        List<DistribucionDimension> dimensiones = cuestionarios.stream()
                .filter(c -> c.metodoCalculo() == MetodoCalculo.SUMA_DIRECTA)
                .flatMap(c -> c.dominios().stream())
                .flatMap(d -> d.dimensiones().stream())
                .map(d -> new DistribucionDimension(d.idDimensionCuestionario(), d.nombre(), distribuir(
                        resultados.stream()
                                .flatMap(rc -> rc.dimensiones().stream())
                                .filter(rd -> rd.idDimensionCuestionario().equals(d.idDimensionCuestionario()))
                                .map(ResultadoDimension::nivelRiesgo)
                                .toList())))
                .filter(d -> d.distribucion().total() > 0)
                .sorted(Comparator.comparing(DistribucionDimension::distribucion, DistribucionNiveles.masCriticaPrimero()))
                .toList();

        Collection<List<ParticipanteEmpresa>> porCiudad = porCiudad(participantes);
        Collection<List<ParticipanteEmpresa>> porArea = porArea(participantes);
        return new ResultadosExtralaborales(dimensiones,
                desglose(porCiudad, AnalizadorInformeEmpresa::nombreCiudad, MetodoCalculo.SUMA_DIRECTA, OTRAS_CIUDADES),
                desglose(porArea, AnalizadorInformeEmpresa::nombreArea, MetodoCalculo.SUMA_DIRECTA, OTRAS_AREAS));
    }

    /**
     * Describe a las personas evaluadas (las que respondieron alguna prueba): forma intralaboral
     * presentada, sexo, rango de edad (año del informe − año de nacimiento), nivel académico, tipo
     * de cargo y tipo de contrato. Porcentajes sobre las personas evaluadas.
     *
     * @param fichas datos generales por {@code idEvaluadoCliente}; sin ficha = todo "Sin dato"
     */
    public DescripcionPoblacion describirPoblacion(List<ParticipanteEmpresa> participantes,
                                                   Map<Long, FichaPersona> fichas, int anio) {
        List<ParticipanteEmpresa> evaluados = participantes.stream().filter(ParticipanteEmpresa::tieneRespuestas).toList();
        Function<ParticipanteEmpresa, FichaPersona> ficha = p -> fichas.getOrDefault(p.idEvaluadoCliente(), FichaPersona.SIN_DATOS);
        int total = evaluados.size();
        return new DescripcionPoblacion(total, List.of(
                variable("Forma intralaboral aplicada", evaluados, this::formaIntralaboral, total),
                variable("Sexo", evaluados, p -> ficha.apply(p).sexo(), total),
                variable("Rango de edad", evaluados, p -> rangoEdad(ficha.apply(p).anioNacimiento(), anio), total),
                variable("Nivel académico", evaluados, p -> ficha.apply(p).nivelAcademico(), total),
                variable("Tipo de cargo", evaluados, p -> ficha.apply(p).tipoCargo(), total),
                variable("Tipo de contrato", evaluados, p -> ficha.apply(p).tipoContrato(), total)));
    }

    private static final Categoria SIN_DATO = new Categoria(Long.MAX_VALUE, "Sin dato");

    /** Cuenta por categoría, en el orden de su id ("Sin dato" al final); solo las que tienen personas. */
    private static VariablePoblacion variable(String nombre, List<ParticipanteEmpresa> evaluados,
                                              Function<ParticipanteEmpresa, Categoria> categoria, int total) {
        Map<Categoria, Long> conteo = evaluados.stream()
                .collect(Collectors.groupingBy(p -> Optional.ofNullable(categoria.apply(p)).orElse(SIN_DATO),
                        Collectors.counting()));
        return new VariablePoblacion(nombre, conteo.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Categoria::id)))
                .map(e -> new ConteoCategoria(e.getKey().nombre(), e.getValue().intValue(),
                        porcentaje(e.getValue().intValue(), total)))
                .toList());
    }

    /** Forma intralaboral del resultado de la persona (A antes que B); sin resultado intralaboral, su propia categoría. */
    private Categoria formaIntralaboral(ParticipanteEmpresa participante) {
        if (participante.resultado() == null) {
            return new Categoria(Long.MAX_VALUE - 1, "Sin resultado intralaboral");
        }
        return participante.resultado().cuestionarios().stream()
                .filter(rc -> cuestionario(rc.idCuestionario()).metodoCalculo() == MetodoCalculo.SUMA_POR_DOMINIOS)
                .findFirst()
                .map(rc -> new Categoria((long) cuestionarios.indexOf(cuestionario(rc.idCuestionario())),
                        "Forma " + rc.forma()))
                .orElse(new Categoria(Long.MAX_VALUE - 1, "Sin resultado intralaboral"));
    }

    /** Rangos de edad del informe; sin año de nacimiento = "Sin dato". */
    static Categoria rangoEdad(Integer anioNacimiento, int anio) {
        if (anioNacimiento == null) {
            return SIN_DATO;
        }
        int edad = anio - anioNacimiento;
        if (edad < 25) {
            return new Categoria(1L, "Menor de 25 años");
        }
        if (edad < 35) {
            return new Categoria(2L, "25 a 34 años");
        }
        if (edad < 45) {
            return new Categoria(3L, "35 a 44 años");
        }
        if (edad < 55) {
            return new Categoria(4L, "45 a 54 años");
        }
        return new Categoria(5L, "55 años o más");
    }

    /**
     * Nivel de riesgo intralaboral de la empresa y periodicidad de la evaluación (Resolución 2764 de
     * 2022, art. 3): por cada forma presentada, el promedio del puntaje bruto total de sus personas,
     * transformado con el factor de la forma y ubicado en su baremo. Anual si alguna forma queda en
     * los dos niveles más altos (alto o muy alto); si no, cada dos años.
     *
     * @param configuraciones factor y baremo total de cada forma intralaboral
     */
    public NivelRiesgoEmpresa nivelRiesgoEmpresa(List<ParticipanteEmpresa> participantes,
                                                 List<ConfiguracionNivelEmpresa> configuraciones) {
        Map<Long, List<ResultadoCuestionario>> porForma = resultadosPorMetodo(participantes, MetodoCalculo.SUMA_POR_DOMINIOS)
                .stream()
                .collect(Collectors.groupingBy(ResultadoCuestionario::idCuestionario));
        List<NivelForma> formas = configuraciones.stream()
                .filter(c -> porForma.containsKey(c.idCuestionario()))
                .sorted(Comparator.comparing(c -> cuestionarios.indexOf(cuestionario(c.idCuestionario()))))
                .map(c -> nivelForma(c, porForma.get(c.idCuestionario())))
                .toList();
        if (formas.isEmpty()) {
            return new NivelRiesgoEmpresa(List.of(), null);
        }
        Set<Long> nivelesAltos = niveles.subList(Math.max(0, niveles.size() - 2), niveles.size()).stream()
                .map(NivelRiesgo::id)
                .collect(Collectors.toSet());
        boolean anual = formas.stream().anyMatch(f -> nivelesAltos.contains(f.nivel().id()));
        return new NivelRiesgoEmpresa(formas, anual ? Periodicidad.ANUAL : Periodicidad.CADA_DOS_ANIOS);
    }

    private static NivelForma nivelForma(ConfiguracionNivelEmpresa configuracion, List<ResultadoCuestionario> resultados) {
        BigDecimal suma = resultados.stream().map(ResultadoCuestionario::puntajeBruto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(BigDecimal.valueOf(resultados.size()), MathContext.DECIMAL64);
        BigDecimal transformado = CalculadoraResultado.transformar(promedio, configuracion.factorTransformacion());
        NivelRiesgo nivel = configuracion.baremos().stream()
                .filter(r -> r.contiene(transformado))
                .map(RangoBaremo::nivelRiesgo)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No hay baremo del total intralaboral de la forma "
                        + configuracion.forma() + " para el puntaje " + transformado
                        + " (nivel de riesgo de la empresa)"));
        return new NivelForma(configuracion.idCuestionario(), configuracion.forma(), resultados.size(),
                promedio.setScale(2, RoundingMode.HALF_UP), transformado, nivel);
    }

    /** Personas agrupadas por ciudad de trabajo (por id: hay municipios homónimos); sin ciudad = un grupo. */
    private static Collection<List<ParticipanteEmpresa>> porCiudad(List<ParticipanteEmpresa> participantes) {
        return participantes.stream()
                .collect(Collectors.groupingBy(p -> Optional.ofNullable(p.ciudad()).map(CiudadTrabajo::id),
                        LinkedHashMap::new, Collectors.toList()))
                .values();
    }

    /** Personas agrupadas por área, sin distinguir mayúsculas, tildes ni espacios repetidos. */
    private static Collection<List<ParticipanteEmpresa>> porArea(List<ParticipanteEmpresa> participantes) {
        return participantes.stream()
                .collect(Collectors.groupingBy(p -> claveArea(p.area()), LinkedHashMap::new, Collectors.toList()))
                .values();
    }

    /** Niveles que obtuvo cada persona de un grupo en una prueba. */
    private record NivelesGrupo(String grupo, List<NivelRiesgo> niveles) {
    }

    /**
     * Una prueba por grupo, de el de más personas al de menos (empate: por nombre). Los grupos con
     * menos de {@link #MINIMO_PERSONAS_GRUPO} personas se juntan al final en {@code nombreOtros}; si
     * aun juntos no llegan al mínimo, se omiten. Los grupos sin resultados de la prueba no aparecen.
     */
    private DesgloseGrupos desglose(Collection<List<ParticipanteEmpresa>> grupos,
                                    Function<List<ParticipanteEmpresa>, String> nombre, MetodoCalculo metodo,
                                    String nombreOtros) {
        Map<Boolean, List<NivelesGrupo>> porTamano = grupos.stream()
                .map(grupo -> new NivelesGrupo(nombre.apply(grupo), nivelesDe(resultadosPorMetodo(grupo, metodo))))
                .filter(g -> !g.niveles().isEmpty())
                .collect(Collectors.partitioningBy(g -> g.niveles().size() >= MINIMO_PERSONAS_GRUPO));

        List<DistribucionGrupo> visibles = new ArrayList<>(porTamano.get(true).stream()
                .map(g -> new DistribucionGrupo(g.grupo(), distribuir(g.niveles())))
                .sorted(Comparator.comparingInt((DistribucionGrupo g) -> g.distribucion().total()).reversed()
                        .thenComparing(DistribucionGrupo::grupo))
                .toList());
        List<NivelesGrupo> pequenos = porTamano.get(false);
        List<NivelRiesgo> nivelesPequenos = pequenos.stream().flatMap(g -> g.niveles().stream()).toList();
        if (nivelesPequenos.size() >= MINIMO_PERSONAS_GRUPO) {
            visibles.add(new DistribucionGrupo(nombreOtros, distribuir(nivelesPequenos)));
            return new DesgloseGrupos(List.copyOf(visibles), pequenos.size(), 0);
        }
        return new DesgloseGrupos(List.copyOf(visibles), 0, nivelesPequenos.size());
    }

    /**
     * Frecuencia de síntomas por dimensión de estrés. Solo cuentan las respuestas de la aplicación
     * de donde sale el resultado de estrés de cada persona, para que coincida con el total.
     *
     * @param respuestas respuestas a los ítems de estrés (pueden traer otras aplicaciones: se descartan)
     * @param opciones   opciones del cuestionario de estrés, de la más frecuente (Siempre) a la menos (Nunca)
     */
    public SintomasEstres sintomasEstres(List<ParticipanteEmpresa> participantes, List<RespuestaSintoma> respuestas,
                                         List<OpcionFrecuencia> opciones) {
        Set<Long> aplicaciones = aplicacionesConEstres(participantes);
        Map<Long, List<RespuestaSintoma>> porDimension = respuestas.stream()
                .filter(r -> aplicaciones.contains(r.idAplicacion()))
                .collect(Collectors.groupingBy(RespuestaSintoma::idDimensionCuestionario));

        List<FrecuenciaDimension> dimensiones = cuestionarios.stream()
                .filter(c -> c.metodoCalculo() == MetodoCalculo.PROMEDIO_PONDERADO)
                .flatMap(c -> c.dominios().stream())
                .flatMap(d -> d.dimensiones().stream())
                .map(d -> frecuencia(d, porDimension.getOrDefault(d.idDimensionCuestionario(), List.of()), opciones))
                .filter(f -> f.respuestas() > 0)
                .sorted(FrecuenciaDimension.masFrecuentePrimero())
                .toList();
        return new SintomasEstres(aplicaciones.size(), dimensiones);
    }

    /** Aplicación de donde sale el resultado de estrés de cada persona que lo tiene. */
    public Set<Long> aplicacionesConEstres(List<ParticipanteEmpresa> participantes) {
        return participantes.stream()
                .filter(p -> p.resultado() != null)
                .filter(p -> p.resultado().cuestionarios().stream()
                        .anyMatch(rc -> cuestionario(rc.idCuestionario()).metodoCalculo() == MetodoCalculo.PROMEDIO_PONDERADO))
                .map(p -> p.resultado().idAplicacion())
                .collect(Collectors.toSet());
    }

    private static FrecuenciaDimension frecuencia(DimensionInforme dimension, List<RespuestaSintoma> respuestas,
                                                  List<OpcionFrecuencia> opciones) {
        Map<Long, Long> cantidadPorOpcion = respuestas.stream()
                .collect(Collectors.groupingBy(RespuestaSintoma::idOpcion, Collectors.counting()));
        int total = respuestas.size();
        List<ConteoFrecuencia> frecuencias = opciones.stream()
                .map(o -> {
                    int cantidad = cantidadPorOpcion.getOrDefault(o.id(), 0L).intValue();
                    return new ConteoFrecuencia(o, cantidad, porcentaje(cantidad, total));
                })
                .toList();
        int personas = (int) respuestas.stream().map(RespuestaSintoma::idAplicacion).distinct().count();
        int items = (int) respuestas.stream().map(RespuestaSintoma::idPregunta).distinct().count();
        return new FrecuenciaDimension(dimension.idDimensionCuestionario(), dimension.nombre(), personas, items,
                total, frecuencias);
    }

    /** Niveles que obtuvieron las personas en un dominio, sea cual sea su forma. */
    private List<NivelRiesgo> nivelesDominio(List<ParticipanteEmpresa> participantes, Long idDominio,
                                             Map<Long, Long> dominioPorDominioCuestionario) {
        return resultadosPorMetodo(participantes, MetodoCalculo.SUMA_POR_DOMINIOS).stream()
                .flatMap(rc -> rc.dominios().stream())
                .filter(rd -> idDominio.equals(dominioPorDominioCuestionario.get(rd.idDominioCuestionario())))
                .map(ResultadoDominio::nivelRiesgo)
                .toList();
    }

    private static String nombreCiudad(List<ParticipanteEmpresa> grupo) {
        CiudadTrabajo ciudad = grupo.get(0).ciudad();
        return ciudad == null ? SIN_CIUDAD : ciudad.nombre();
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
