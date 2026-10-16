package com.riesgopsicosocial.domain.service.informe;

import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RangoBaremo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AnalizadorInformeEmpresaTest {

    private static final NivelRiesgo SIN_RIESGO = new NivelRiesgo(1L, "Sin riesgo o riesgo despreciable");
    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo MEDIO = new NivelRiesgo(3L, "Riesgo medio");
    private static final NivelRiesgo ALTO = new NivelRiesgo(4L, "Riesgo alto");
    private static final NivelRiesgo MUY_ALTO = new NivelRiesgo(5L, "Riesgo muy alto");
    private static final List<NivelRiesgo> NIVELES = List.of(SIN_RIESGO, BAJO, MEDIO, ALTO, MUY_ALTO);

    private static final CuestionarioInforme FORMA_A = new CuestionarioInforme(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS,
            List.of(new DominioInforme(10L, 1L, "Liderazgo", List.of()), new DominioInforme(11L, 2L, "Control", List.of())));
    private static final CuestionarioInforme FORMA_B = new CuestionarioInforme(2L, "B", MetodoCalculo.SUMA_POR_DOMINIOS,
            List.of(new DominioInforme(20L, 1L, "Liderazgo", List.of()), new DominioInforme(21L, 2L, "Control", List.of())));
    private static final CuestionarioInforme FORMA_C = new CuestionarioInforme(3L, "C", MetodoCalculo.SUMA_DIRECTA, List.of());
    private static final CuestionarioInforme FORMA_D = new CuestionarioInforme(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO, List.of());

    private final AnalizadorInformeEmpresa analizador =
            new AnalizadorInformeEmpresa(List.of(FORMA_A, FORMA_B, FORMA_C, FORMA_D), NIVELES);

    @Test
    void cobertura_cuentaEsperadosEvaluadosYPendientes() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, "Comercial", resultado(intra(FORMA_A, ALTO, BAJO, MEDIO))),
                new ParticipanteEmpresa(2L, "Comercial", null, true, null, FiltrosAplicacion.SIN_REGISTRO),   // respondió, sin calcular
                new ParticipanteEmpresa(3L, "Comercial", null, false, null, FiltrosAplicacion.SIN_REGISTRO)); // no se ha evaluado

        AnalisisGrupo analisis = analizador.analizar(participantes);

        assertEquals(new Cobertura(3, 2, bd("66.7")), analisis.cobertura());
        assertEquals(1, analisis.pendientesCalculo());
        assertEquals(1, analisis.intralaboral().total());
    }

    @Test
    void globales_distribuyenCadaCuestionarioSobreSuPropioTotal() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(intra(FORMA_A, ALTO, ALTO, ALTO), total(FORMA_C, BAJO), total(FORMA_D, MUY_ALTO))),
                participante(2L, null, resultado(intra(FORMA_B, ALTO, ALTO, ALTO), total(FORMA_C, MEDIO))),
                participante(3L, null, resultado(intra(FORMA_A, SIN_RIESGO, SIN_RIESGO, SIN_RIESGO))));

        AnalisisGrupo analisis = analizador.analizar(participantes);

        // Intralaboral junta A y B
        assertEquals(3, analisis.intralaboral().total());
        assertEquals(List.of(1, 0, 0, 2, 0), cantidades(analisis.intralaboral()));
        assertEquals(List.of(bd("33.3"), bd("0.0"), bd("0.0"), bd("66.7"), bd("0.0")), porcentajes(analisis.intralaboral()));

        assertEquals(2, analisis.extralaboral().total());
        assertEquals(List.of(0, 1, 1, 0, 0), cantidades(analisis.extralaboral()));
        assertEquals(List.of(bd("0.0"), bd("50.0"), bd("50.0"), bd("0.0"), bd("0.0")), porcentajes(analisis.extralaboral()));

        assertEquals(1, analisis.estres().total());
        assertEquals(List.of(0, 0, 0, 0, 1), cantidades(analisis.estres()));
        assertEquals(bd("100.0"), analisis.estres().niveles().get(4).porcentaje());
    }

    @Test
    void dominios_porFormaYSoloLasFormasPresentadas() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(intra(FORMA_A, ALTO, ALTO, BAJO))),
                participante(2L, null, resultado(intra(FORMA_A, MEDIO, MUY_ALTO, BAJO))));

        List<DominiosForma> dominios = analizador.analizar(participantes).dominios();

        assertEquals(1, dominios.size(), "B no se presentó: no debe aparecer");
        DominiosForma formaA = dominios.get(0);
        assertEquals("A", formaA.forma());
        assertEquals(2, formaA.total());
        assertEquals(List.of("Liderazgo", "Control"), formaA.dominios().stream().map(DistribucionDominio::nombre).toList());
        assertEquals(List.of(0, 0, 0, 1, 1), cantidades(formaA.dominios().get(0).distribucion()));
        assertEquals(List.of(0, 2, 0, 0, 0), cantidades(formaA.dominios().get(1).distribucion()));
    }

    @Test
    void dominios_rankingPorMuyAltoLuegoAltoYAsiHaciaAbajo_empateEnOrdenDelCuestionario() {
        CuestionarioInforme forma = new CuestionarioInforme(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS,
                List.of(new DominioInforme(10L, 1L, "Liderazgo", List.of()), new DominioInforme(11L, 2L, "Control", List.of()),
                        new DominioInforme(12L, 3L, "Demandas", List.of()), new DominioInforme(13L, 4L, "Recompensas", List.of())));
        AnalizadorInformeEmpresa conCuatroDominios = new AnalizadorInformeEmpresa(List.of(forma), NIVELES);
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(intra(forma, MEDIO, BAJO, ALTO, BAJO, MUY_ALTO))),
                participante(2L, null, resultado(intra(forma, MEDIO, BAJO, MEDIO, BAJO, ALTO))),
                participante(3L, null, resultado(intra(forma, MEDIO, SIN_RIESGO, BAJO, SIN_RIESGO, MEDIO))));

        DominiosForma ranking = conCuatroDominios.analizar(participantes).dominios().get(0);

        // Recompensas tiene muy alto; Control no, pero sí alto; Liderazgo y Demandas empatan en todo.
        assertEquals(List.of("Recompensas", "Control", "Liderazgo", "Demandas"),
                ranking.dominios().stream().map(DistribucionDominio::nombre).toList());
    }

    @Test
    void dimensiones_rankingDentroDelDominioYSinContarAQuienNoLeAplicaLaPreguntaFiltro() {
        DimensionInforme caracteristicas = new DimensionInforme(100L, "Características del liderazgo", null);
        DimensionInforme colaboradores = new DimensionInforme(101L, "Relación con los colaboradores",
                CondicionAplicacion.ES_JEFE);
        CuestionarioInforme forma = new CuestionarioInforme(1L, "A", MetodoCalculo.SUMA_POR_DOMINIOS,
                List.of(new DominioInforme(10L, 1L, "Liderazgo", List.of(caracteristicas, colaboradores))));
        AnalizadorInformeEmpresa conDimensiones = new AnalizadorInformeEmpresa(List.of(forma), NIVELES);
        List<ParticipanteEmpresa> participantes = List.of(
                conFiltros(1L, new FiltrosAplicacion(null, true), conDimension(forma, BAJO, MUY_ALTO)),
                conFiltros(2L, new FiltrosAplicacion(null, false), conDimension(forma, ALTO, SIN_RIESGO)), // no es jefe
                conFiltros(3L, FiltrosAplicacion.SIN_REGISTRO, conDimension(forma, BAJO, ALTO)));          // no registrado

        DistribucionDominio liderazgo = conDimensiones.analizar(participantes).dominios().get(0).dominios().get(0);

        assertEquals(List.of("Relación con los colaboradores", "Características del liderazgo"),
                liderazgo.dimensiones().stream().map(DistribucionDimension::nombre).toList());
        DistribucionDimension relacion = liderazgo.dimensiones().get(0);
        assertEquals(2, relacion.distribucion().total(), "el que no es jefe no cuenta");
        assertEquals(List.of(0, 0, 0, 1, 1), cantidades(relacion.distribucion()));
        assertEquals(3, liderazgo.dimensiones().get(1).distribucion().total());
    }

    @Test
    void masCriticaPrimero_comparaDesdeMuyAltoHaciaAbajoSinSumar() {
        // A: 40 % alto + 0 % muy alto; B: 10 % muy alto. Sumando, A iría primero (40 > 10); sin sumar, B.
        DistribucionNiveles a = distribucion("60.0", "0.0", "0.0", "40.0", "0.0");
        DistribucionNiveles b = distribucion("90.0", "0.0", "0.0", "0.0", "10.0");
        DistribucionNiveles c = distribucion("50.0", "0.0", "10.0", "40.0", "0.0");

        assertEquals(List.of(b, c, a), Stream.of(a, b, c)
                .sorted(DistribucionNiveles.masCriticaPrimero()).toList());
    }

    @Test
    void grupoSinResultados_tablasEnCeroSinDividirPorCero() {
        AnalisisGrupo analisis = analizador.analizar(List.of(new ParticipanteEmpresa(1L, "X", null, false, null, FiltrosAplicacion.SIN_REGISTRO)));

        assertEquals(new Cobertura(1, 0, bd("0.0")), analisis.cobertura());
        assertEquals(0, analisis.intralaboral().total());
        assertEquals(5, analisis.intralaboral().niveles().size());
        assertTrue(analisis.intralaboral().niveles().stream().allMatch(c -> c.cantidad() == 0 && c.porcentaje().equals(bd("0.0"))));
        assertTrue(analisis.dominios().isEmpty());
    }

    @Test
    void areas_seAgrupanSinMayusculasTildesNiEspaciosYSeOrdenanPorEsperados() {
        List<ParticipanteEmpresa> participantes = List.of(
                new ParticipanteEmpresa(1L, "VENTAS", null, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(2L, " Ventas ", null, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(3L, "Ventas", null, true, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(4L, "Dirección", null, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(5L, "direccion", null, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(6L, null, null, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(7L, "  ", null, false, null, FiltrosAplicacion.SIN_REGISTRO));

        List<AnalisisArea> areas = analizador.analizarPorArea(participantes);

        assertEquals(List.of("Ventas", "Dirección", "Sin área"), areas.stream().map(AnalisisArea::area).toList());
        assertEquals(new Cobertura(3, 1, bd("33.3")), areas.get(0).analisis().cobertura());
        assertEquals(2, areas.get(1).analisis().cobertura().esperados());
        assertEquals(2, areas.get(2).analisis().cobertura().esperados());
    }

    @Test
    void coberturaPorCiudad_agrupaPorIdYOrdenaPorEsperados() {
        CiudadTrabajo bogota = new CiudadTrabajo(1L, "BOGOTA, D.C.");
        CiudadTrabajo albanCundinamarca = new CiudadTrabajo(2L, "ALBAN");
        CiudadTrabajo albanNarino = new CiudadTrabajo(3L, "ALBAN");
        List<ParticipanteEmpresa> participantes = List.of(
                new ParticipanteEmpresa(1L, null, bogota, true, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(2L, null, bogota, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(3L, null, bogota, true, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(4L, null, albanCundinamarca, true, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(5L, null, albanNarino, false, null, FiltrosAplicacion.SIN_REGISTRO),
                new ParticipanteEmpresa(6L, null, null, false, null, FiltrosAplicacion.SIN_REGISTRO));

        List<CoberturaCiudad> ciudades = analizador.analizar(participantes).coberturaPorCiudad();

        assertEquals(4, ciudades.size(), "municipios homónimos de distinto departamento no se juntan");
        assertEquals(new CoberturaCiudad(1L, "BOGOTA, D.C.", new Cobertura(3, 2, bd("66.7"))), ciudades.get(0));
        assertEquals(new CoberturaCiudad(2L, "ALBAN", new Cobertura(1, 1, bd("100.0"))), ciudades.get(1));
        assertEquals(new CoberturaCiudad(3L, "ALBAN", new Cobertura(1, 0, bd("0.0"))), ciudades.get(2));
        assertEquals(new CoberturaCiudad(null, "Sin ciudad", new Cobertura(1, 0, bd("0.0"))), ciudades.get(3));
    }

    @Test
    void concentracion_juntaAYBPorDominioComunEnRanking() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(intra(FORMA_A, ALTO, BAJO, MUY_ALTO))),
                participante(2L, null, resultado(intra(FORMA_B, MEDIO, SIN_RIESGO, ALTO))),
                participante(3L, null, resultado(intra(FORMA_B, BAJO, BAJO, MEDIO))),
                participante(4L, null, resultado(total(FORMA_C, MUY_ALTO))));   // sin intralaboral: no cuenta

        List<DominioConsolidado> dominios = analizador.concentracionRiesgo(participantes).dominios();

        // Control tiene muy alto: va primero aunque en el cuestionario esté después de Liderazgo.
        assertEquals(List.of("Control", "Liderazgo"), dominios.stream().map(DominioConsolidado::nombre).toList());
        assertEquals(List.of(2L, 1L), dominios.stream().map(DominioConsolidado::idDominio).toList());
        assertEquals(3, dominios.get(0).distribucion().total());
        assertEquals(List.of(0, 0, 1, 1, 1), cantidades(dominios.get(0).distribucion()));
        assertEquals(List.of(1, 2, 0, 0, 0), cantidades(dominios.get(1).distribucion()));
    }

    @Test
    void concentracion_porCiudadYArea_cadaPruebaConSuDesgloseYGruposPequenosProtegidos() {
        CiudadTrabajo bogota = new CiudadTrabajo(1L, "BOGOTA, D.C.");
        CiudadTrabajo cali = new CiudadTrabajo(2L, "CALI");
        List<ParticipanteEmpresa> participantes = List.of(
                enGrupo(1L, "Ventas", bogota, resultado(intra(FORMA_A, ALTO, MUY_ALTO, BAJO))),
                enGrupo(2L, "ventas", bogota, resultado(intra(FORMA_B, MEDIO, ALTO, SIN_RIESGO))),
                enGrupo(3L, "Operaciones", bogota, resultado(intra(FORMA_B, MUY_ALTO, MEDIO, ALTO))),
                enGrupo(4L, "Operaciones", cali, resultado(intra(FORMA_A, BAJO, BAJO, BAJO), total(FORMA_D, MUY_ALTO))),
                enGrupo(5L, "Gerencia", null, resultado(total(FORMA_D, ALTO))),
                enGrupo(6L, "Dirección", null, null));   // sin resultados: no aparece en ninguna prueba

        ConcentracionRiesgo concentracion = analizador.concentracionRiesgo(participantes);

        // Intralaboral por ciudad: Cali tiene 1 persona y es el único grupo pequeño: se omite.
        DesgloseGrupos intraCiudad = concentracion.intralaboralPorCiudad();
        assertEquals(List.of("BOGOTA, D.C."), intraCiudad.grupos().stream().map(DistribucionGrupo::grupo).toList());
        assertEquals(List.of(0, 0, 1, 1, 1), cantidades(intraCiudad.grupos().get(0).distribucion()));
        assertEquals(List.of(bd("0.0"), bd("0.0"), bd("33.3"), bd("33.3"), bd("33.3")),
                porcentajes(intraCiudad.grupos().get(0).distribucion()));
        assertEquals(0, intraCiudad.agrupados());
        assertEquals(1, intraCiudad.omitidos());

        // Estrés por ciudad: Cali (1) y Sin ciudad (1) juntos llegan al mínimo: "Otras ciudades".
        DesgloseGrupos estresCiudad = concentracion.estresPorCiudad();
        assertEquals(List.of("Otras ciudades"), estresCiudad.grupos().stream().map(DistribucionGrupo::grupo).toList());
        assertEquals(List.of(0, 0, 0, 1, 1), cantidades(estresCiudad.grupos().get(0).distribucion()));
        assertEquals(2, estresCiudad.agrupados());
        assertEquals(0, estresCiudad.omitidos());

        // Intralaboral por área: empate en personas, por nombre; ninguna es pequeña.
        DesgloseGrupos intraArea = concentracion.intralaboralPorArea();
        assertEquals(List.of("Operaciones", "Ventas"), intraArea.grupos().stream().map(DistribucionGrupo::grupo).toList());
        assertEquals(List.of(0, 1, 0, 0, 1), cantidades(intraArea.grupos().get(0).distribucion()));
        assertEquals(new DesgloseGrupos(intraArea.grupos(), 0, 0), intraArea);

        assertEquals(List.of("Otras áreas"),
                concentracion.estresPorArea().grupos().stream().map(DistribucionGrupo::grupo).toList());
    }

    @Test
    void concentracion_otrasVaAlFinalDespuesDeLosGruposVisibles() {
        CiudadTrabajo bogota = new CiudadTrabajo(1L, "BOGOTA, D.C.");
        List<ParticipanteEmpresa> participantes = List.of(
                enGrupo(1L, "X", bogota, resultado(intra(FORMA_A, ALTO, ALTO, ALTO))),
                enGrupo(2L, "X", bogota, resultado(intra(FORMA_A, ALTO, ALTO, ALTO))),
                enGrupo(3L, "X", new CiudadTrabajo(2L, "CALI"), resultado(intra(FORMA_A, BAJO, BAJO, BAJO))),
                enGrupo(4L, "X", new CiudadTrabajo(3L, "PASTO"), resultado(intra(FORMA_A, BAJO, BAJO, BAJO))),
                enGrupo(5L, "X", new CiudadTrabajo(4L, "TULUA"), resultado(intra(FORMA_A, BAJO, BAJO, BAJO))));

        DesgloseGrupos porCiudad = analizador.concentracionRiesgo(participantes).intralaboralPorCiudad();

        // "Otras ciudades" tiene más personas (3) que Bogotá (2), pero va al final.
        assertEquals(List.of("BOGOTA, D.C.", "Otras ciudades"),
                porCiudad.grupos().stream().map(DistribucionGrupo::grupo).toList());
        assertEquals(3, porCiudad.grupos().get(1).distribucion().total());
        assertEquals(3, porCiudad.agrupados());
    }

    @Test
    void concentracion_sinResultadosIntralaborales_quedaVacia() {
        ConcentracionRiesgo concentracion = analizador.concentracionRiesgo(List.of(
                participante(1L, "X", resultado(total(FORMA_C, ALTO)))));

        assertTrue(concentracion.dominios().isEmpty());
        assertTrue(concentracion.intralaboralPorCiudad().grupos().isEmpty());
        assertTrue(concentracion.intralaboralPorArea().grupos().isEmpty());
    }

    @Test
    void sintomasEstres_frecuenciaPorDimensionSoloDeLaAplicacionQueDaElResultado() {
        DimensionInforme fisiologicos = new DimensionInforme(43L, "Síntomas fisiológicos", null);
        DimensionInforme psicoemocionales = new DimensionInforme(46L, "Síntomas psicoemocionales", null);
        CuestionarioInforme estres = new CuestionarioInforme(4L, "D", MetodoCalculo.PROMEDIO_PONDERADO,
                List.of(new DominioInforme(10L, 6L, "Estrés", List.of(fisiologicos, psicoemocionales))));
        AnalizadorInformeEmpresa conEstres = new AnalizadorInformeEmpresa(List.of(estres), NIVELES);
        OpcionFrecuencia siempre = new OpcionFrecuencia(1L, "Siempre");
        OpcionFrecuencia casiSiempre = new OpcionFrecuencia(2L, "Casi siempre");
        OpcionFrecuencia nunca = new OpcionFrecuencia(5L, "Nunca");
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, new ResultadoAplicacion(100L, 1L, null, List.of(total(estres, ALTO)), null)),
                participante(2L, null, new ResultadoAplicacion(200L, 1L, null, List.of(total(estres, BAJO)), null)));
        List<RespuestaSintoma> respuestas = List.of(
                new RespuestaSintoma(100L, 1L, 43L, 1L), new RespuestaSintoma(100L, 2L, 43L, 5L),
                new RespuestaSintoma(200L, 1L, 43L, 5L), new RespuestaSintoma(200L, 2L, 43L, 5L),
                new RespuestaSintoma(100L, 3L, 46L, 2L), new RespuestaSintoma(200L, 3L, 46L, 5L),
                new RespuestaSintoma(999L, 3L, 46L, 1L));   // otra aplicación (p. ej. una anterior): no cuenta

        SintomasEstres sintomas = conEstres.sintomasEstres(participantes, respuestas, List.of(siempre, casiSiempre, nunca));

        assertEquals(2, sintomas.personas());
        // Fisiológicos tiene "Siempre": va primero aunque psicoemocionales tenga más "Casi siempre".
        assertEquals(List.of("Síntomas fisiológicos", "Síntomas psicoemocionales"),
                sintomas.dimensiones().stream().map(FrecuenciaDimension::nombre).toList());
        FrecuenciaDimension primera = sintomas.dimensiones().get(0);
        assertEquals(4, primera.respuestas());
        assertEquals(2, primera.personas());
        assertEquals(2, primera.items(), "respuestas = personas × ítems");
        assertEquals(List.of(bd("25.0"), bd("0.0"), bd("75.0")),
                primera.frecuencias().stream().map(ConteoFrecuencia::porcentaje).toList());
        FrecuenciaDimension segunda = sintomas.dimensiones().get(1);
        assertEquals(2, segunda.respuestas());
        assertEquals(List.of(0, 1, 1), segunda.frecuencias().stream().map(ConteoFrecuencia::cantidad).toList());
    }

    @Test
    void sintomasEstres_sinResultadosDeEstres_quedaVacio() {
        SintomasEstres sintomas = analizador.sintomasEstres(
                List.of(participante(1L, null, resultado(intra(FORMA_A, ALTO, ALTO, ALTO)))), List.of(), List.of());

        assertEquals(0, sintomas.personas());
        assertTrue(sintomas.dimensiones().isEmpty());
    }

    @Test
    void extralaboral_rankingDeDimensionesYTotalPorCiudadYArea() {
        DimensionInforme vivienda = new DimensionInforme(40L, "Características de la vivienda", null);
        DimensionInforme desplazamiento = new DimensionInforme(42L, "Desplazamiento", null);
        CuestionarioInforme extra = new CuestionarioInforme(3L, "C", MetodoCalculo.SUMA_DIRECTA,
                List.of(new DominioInforme(9L, 5L, "Condiciones extralaborales", List.of(vivienda, desplazamiento))));
        AnalizadorInformeEmpresa conExtra = new AnalizadorInformeEmpresa(List.of(FORMA_A, extra), NIVELES);
        CiudadTrabajo bogota = new CiudadTrabajo(1L, "BOGOTA, D.C.");
        List<ParticipanteEmpresa> participantes = List.of(
                enGrupo(1L, "Ventas", bogota, resultado(extralaboral(extra, MEDIO, BAJO, MUY_ALTO))),
                enGrupo(2L, "Ventas", bogota, resultado(extralaboral(extra, ALTO, ALTO, BAJO))),
                enGrupo(3L, "Ventas", new CiudadTrabajo(2L, "CALI"), resultado(extralaboral(extra, BAJO, BAJO, SIN_RIESGO))),
                enGrupo(4L, "Ventas", bogota, resultado(intra(FORMA_A, ALTO, ALTO, ALTO))));   // sin extralaboral

        ResultadosExtralaborales resultados = conExtra.extralaboral(participantes);

        // Desplazamiento tiene muy alto: va primero aunque en el cuestionario esté después.
        assertEquals(List.of("Desplazamiento", "Características de la vivienda"),
                resultados.dimensiones().stream().map(DistribucionDimension::nombre).toList());
        assertEquals(List.of(1, 1, 0, 0, 1), cantidades(resultados.dimensiones().get(0).distribucion()));
        assertEquals(List.of(0, 2, 0, 1, 0), cantidades(resultados.dimensiones().get(1).distribucion()));

        // Cali (1) es el único grupo pequeño: se omite.
        assertEquals(List.of("BOGOTA, D.C."), resultados.porCiudad().grupos().stream().map(DistribucionGrupo::grupo).toList());
        assertEquals(List.of(0, 0, 1, 1, 0), cantidades(resultados.porCiudad().grupos().get(0).distribucion()));
        assertEquals(1, resultados.porCiudad().omitidos());
        assertEquals(3, resultados.porArea().grupos().get(0).distribucion().total());
    }

    /** Resultado extralaboral con su total y el nivel de cada dimensión, en el orden del cuestionario. */
    private static ResultadoCuestionario extralaboral(CuestionarioInforme forma, NivelRiesgo total,
                                                      NivelRiesgo... nivelesDimensiones) {
        DominioInforme dominio = forma.dominios().get(0);
        List<ResultadoDimension> dimensiones = new ArrayList<>();
        for (int i = 0; i < nivelesDimensiones.length; i++) {
            DimensionInforme dimension = dominio.dimensiones().get(i);
            dimensiones.add(new ResultadoDimension(dimension.idDimensionCuestionario(), dominio.idDominioCuestionario(),
                    dimension.nombre(), bd("0"), bd("0"), nivelesDimensiones[i]));
        }
        return new ResultadoCuestionario(forma.idCuestionario(), forma.forma(), bd("0"), bd("0"), total, List.of(),
                dimensiones);
    }

    @Test
    void poblacion_describeSoloALosEvaluadosConCategoriasEnOrdenYSinDatoAlFinal() {
        Categoria femenino = new Categoria(1L, "Femenino");
        Categoria masculino = new Categoria(2L, "Masculino");
        Categoria profesional = new Categoria(2L, "Profesional");
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(intra(FORMA_B, ALTO))),
                participante(2L, null, resultado(intra(FORMA_A, ALTO))),
                participante(3L, null, resultado(intra(FORMA_A, BAJO))),
                new ParticipanteEmpresa(4L, null, null, true, null, FiltrosAplicacion.SIN_REGISTRO),   // pendiente de cálculo
                new ParticipanteEmpresa(5L, null, null, false, null, FiltrosAplicacion.SIN_REGISTRO)); // no evaluado
        Map<Long, FichaPersona> fichas = Map.of(
                1L, new FichaPersona(masculino, 2000, null, profesional, null),
                2L, new FichaPersona(femenino, 1980, null, profesional, null),
                3L, new FichaPersona(femenino, 1971, null, null, null),
                5L, new FichaPersona(masculino, 1990, null, profesional, null));   // 4 sin ficha

        DescripcionPoblacion poblacion = analizador.describirPoblacion(participantes, fichas, 2026);

        assertEquals(4, poblacion.personas(), "el no evaluado no cuenta");
        assertEquals(List.of("Forma intralaboral aplicada", "Sexo", "Rango de edad", "Nivel académico", "Tipo de cargo",
                "Tipo de contrato"), poblacion.variables().stream().map(VariablePoblacion::nombre).toList());
        assertEquals(List.of(new ConteoCategoria("Forma A", 2, bd("50.0")), new ConteoCategoria("Forma B", 1, bd("25.0")),
                new ConteoCategoria("Sin resultado intralaboral", 1, bd("25.0"))), poblacion.variables().get(0).categorias());
        assertEquals(List.of(new ConteoCategoria("Femenino", 2, bd("50.0")), new ConteoCategoria("Masculino", 1, bd("25.0")),
                new ConteoCategoria("Sin dato", 1, bd("25.0"))), poblacion.variables().get(1).categorias());
        // 2026 − 2000 = 26; − 1980 = 46; − 1971 = 55
        assertEquals(List.of("25 a 34 años", "45 a 54 años", "55 años o más", "Sin dato"),
                poblacion.variables().get(2).categorias().stream().map(ConteoCategoria::categoria).toList());
        assertEquals(List.of(new ConteoCategoria("Sin dato", 4, bd("100.0"))), poblacion.variables().get(3).categorias());
    }

    @Test
    void rangoEdad_limitesDeCadaRango() {
        assertEquals("Menor de 25 años", AnalizadorInformeEmpresa.rangoEdad(2002, 2026).nombre());
        assertEquals("25 a 34 años", AnalizadorInformeEmpresa.rangoEdad(2001, 2026).nombre());
        assertEquals("25 a 34 años", AnalizadorInformeEmpresa.rangoEdad(1992, 2026).nombre());
        assertEquals("35 a 44 años", AnalizadorInformeEmpresa.rangoEdad(1991, 2026).nombre());
        assertEquals("45 a 54 años", AnalizadorInformeEmpresa.rangoEdad(1972, 2026).nombre());
        assertEquals("55 años o más", AnalizadorInformeEmpresa.rangoEdad(1971, 2026).nombre());
        assertEquals("Sin dato", AnalizadorInformeEmpresa.rangoEdad(null, 2026).nombre());
    }

    @Test
    void totalGeneral_soloQuienesTienenIntralaboralYExtralaboral() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, conTotalGeneral(ALTO, intra(FORMA_A, ALTO), total(FORMA_C, BAJO))),
                participante(2L, null, conTotalGeneral(MUY_ALTO, intra(FORMA_B, MUY_ALTO), total(FORMA_C, ALTO))),
                participante(3L, null, resultado(intra(FORMA_A, MEDIO))));   // sin extralaboral: sin total general

        DistribucionNiveles totalGeneral = analizador.analizar(participantes).totalGeneral();

        assertEquals(2, totalGeneral.total());
        assertEquals(List.of(0, 0, 0, 1, 1), cantidades(totalGeneral));
        assertEquals(bd("50.0"), totalGeneral.niveles().get(4).porcentaje());
    }

    private static ResultadoAplicacion conTotalGeneral(NivelRiesgo nivel, ResultadoCuestionario... cuestionarios) {
        return new ResultadoAplicacion(1L, 1L, null, List.of(cuestionarios),
                new ResultadoTotalGeneral(cuestionarios[0].idCuestionario(), cuestionarios[0].forma(), bd("0"), bd("0"), nivel));
    }

    private static final List<RangoBaremo> BAREMO_A = List.of(
            rango("0.0", "19.7", SIN_RIESGO), rango("19.8", "25.8", BAJO), rango("25.9", "31.5", MEDIO),
            rango("31.6", "38.0", ALTO), rango("38.1", "100.0", MUY_ALTO));
    private static final List<RangoBaremo> BAREMO_B = List.of(
            rango("0.0", "20.6", SIN_RIESGO), rango("20.7", "26.0", BAJO), rango("26.1", "31.2", MEDIO),
            rango("31.3", "38.7", ALTO), rango("38.8", "100.0", MUY_ALTO));
    private static final List<ConfiguracionNivelEmpresa> CONFIGURACION = List.of(
            new ConfiguracionNivelEmpresa(1L, "A", bd("492"), BAREMO_A),
            new ConfiguracionNivelEmpresa(2L, "B", bd("388"), BAREMO_B));

    @Test
    void nivelEmpresa_promedioDelBrutoTransformadoYUbicadoEnElBaremoDeCadaForma_cadaDosAnios() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(conBruto(FORMA_A, "130"))),
                participante(2L, null, resultado(conBruto(FORMA_A, "143.17"))),
                participante(3L, null, resultado(conBruto(FORMA_B, "100"))),
                participante(4L, null, resultado(total(FORMA_C, MUY_ALTO))));   // sin intralaboral: no cuenta

        NivelRiesgoEmpresa nivel = analizador.nivelRiesgoEmpresa(participantes, CONFIGURACION);

        // A: (130 + 143,17) / 2 = 136,585 → 136,585 / 492 × 100 = 27,76… → 27,8 (medio)
        NivelForma formaA = nivel.formas().get(0);
        assertEquals("A", formaA.forma());
        assertEquals(2, formaA.personas());
        assertEquals(bd("136.59"), formaA.promedioPuntajeBruto());
        assertEquals(bd("27.8"), formaA.puntajeTransformado());
        assertEquals(MEDIO, formaA.nivel());
        // B: 100 / 388 × 100 = 25,77… → 25,8 (bajo)
        assertEquals(BAJO, nivel.formas().get(1).nivel());
        assertEquals(Periodicidad.CADA_DOS_ANIOS, nivel.periodicidad());
    }

    @Test
    void nivelEmpresa_unaFormaEnAltoOMuyAlto_anual() {
        List<ParticipanteEmpresa> participantes = List.of(
                participante(1L, null, resultado(conBruto(FORMA_A, "100"))),      // 20,3: bajo
                participante(2L, null, resultado(conBruto(FORMA_B, "130"))));      // 33,5: alto

        NivelRiesgoEmpresa nivel = analizador.nivelRiesgoEmpresa(participantes, CONFIGURACION);

        assertEquals(List.of(BAJO, ALTO), nivel.formas().stream().map(NivelForma::nivel).toList());
        assertEquals(Periodicidad.ANUAL, nivel.periodicidad());
    }

    @Test
    void nivelEmpresa_soloFormasPresentadas_ySinIntralaboralSinPeriodicidad() {
        NivelRiesgoEmpresa soloB = analizador.nivelRiesgoEmpresa(
                List.of(participante(1L, null, resultado(conBruto(FORMA_B, "50")))), CONFIGURACION);
        assertEquals(List.of("B"), soloB.formas().stream().map(NivelForma::forma).toList());

        NivelRiesgoEmpresa sinIntra = analizador.nivelRiesgoEmpresa(
                List.of(participante(1L, null, resultado(total(FORMA_C, ALTO)))), CONFIGURACION);
        assertTrue(sinIntra.formas().isEmpty());
        assertNull(sinIntra.periodicidad());
    }

    @Test
    void nivelEmpresa_sinBaremoParaElPuntaje_lanzaReglaNegocio() {
        List<ConfiguracionNivelEmpresa> sinBaremos = List.of(new ConfiguracionNivelEmpresa(1L, "A", bd("492"), List.of()));

        assertThrows(ReglaNegocioException.class, () -> analizador.nivelRiesgoEmpresa(
                List.of(participante(1L, null, resultado(conBruto(FORMA_A, "100")))), sinBaremos));
    }

    private static ResultadoCuestionario conBruto(CuestionarioInforme forma, String bruto) {
        return new ResultadoCuestionario(forma.idCuestionario(), forma.forma(), bd(bruto), bd("0"), MEDIO, List.of(), List.of());
    }

    private static RangoBaremo rango(String minimo, String maximo, NivelRiesgo nivel) {
        return new RangoBaremo(bd(minimo), bd(maximo), nivel);
    }

    @Test
    void cuestionarioSinConfiguracion_lanzaReglaNegocio() {
        CuestionarioInforme desconocido = new CuestionarioInforme(99L, "Z", MetodoCalculo.SUMA_DIRECTA, List.of());
        List<ParticipanteEmpresa> participantes = List.of(participante(1L, null, resultado(total(desconocido, BAJO))));

        assertThrows(ReglaNegocioException.class, () -> analizador.analizar(participantes));
    }

    private static ParticipanteEmpresa participante(Long id, String area, ResultadoAplicacion resultado) {
        return new ParticipanteEmpresa(id, area, null, true, resultado, FiltrosAplicacion.SIN_REGISTRO);
    }

    private static ParticipanteEmpresa enGrupo(Long id, String area, CiudadTrabajo ciudad, ResultadoAplicacion resultado) {
        return new ParticipanteEmpresa(id, area, ciudad, true, resultado, FiltrosAplicacion.SIN_REGISTRO);
    }

    private static ResultadoAplicacion resultado(ResultadoCuestionario... cuestionarios) {
        return new ResultadoAplicacion(1L, 1L, null, List.of(cuestionarios), null);
    }

    /** Resultado intralaboral con su total y el nivel de cada dominio, en el orden del cuestionario. */
    private static ResultadoCuestionario intra(CuestionarioInforme forma, NivelRiesgo total,
                                               NivelRiesgo... nivelesDominios) {
        List<ResultadoDominio> dominios = new ArrayList<>();
        for (int i = 0; i < nivelesDominios.length; i++) {
            dominios.add(dominio(forma.dominios().get(i), nivelesDominios[i]));
        }
        return new ResultadoCuestionario(forma.idCuestionario(), forma.forma(), bd("0"), bd("0"), total, dominios, List.of());
    }

    private static ParticipanteEmpresa conFiltros(Long id, FiltrosAplicacion filtros, ResultadoCuestionario intra) {
        return new ParticipanteEmpresa(id, null, null, true, resultado(intra), filtros);
    }

    /** Resultado con un solo dominio (nivel medio) y el nivel de cada una de sus dimensiones. */
    private static ResultadoCuestionario conDimension(CuestionarioInforme forma, NivelRiesgo... nivelesDimensiones) {
        DominioInforme dominio = forma.dominios().get(0);
        List<ResultadoDimension> dimensiones = new ArrayList<>();
        for (int i = 0; i < nivelesDimensiones.length; i++) {
            DimensionInforme dimension = dominio.dimensiones().get(i);
            dimensiones.add(new ResultadoDimension(dimension.idDimensionCuestionario(), dominio.idDominioCuestionario(),
                    dimension.nombre(), bd("0"), bd("0"), nivelesDimensiones[i]));
        }
        return new ResultadoCuestionario(forma.idCuestionario(), forma.forma(), bd("0"), bd("0"), MEDIO,
                List.of(dominio(dominio, MEDIO)), dimensiones);
    }

    private static ResultadoDominio dominio(DominioInforme dominio, NivelRiesgo nivel) {
        return new ResultadoDominio(dominio.idDominioCuestionario(), dominio.nombre(), bd("0"), bd("0"), nivel);
    }

    private static ResultadoCuestionario total(CuestionarioInforme forma, NivelRiesgo nivel) {
        return new ResultadoCuestionario(forma.idCuestionario(), forma.forma(), bd("0"), bd("0"), nivel, List.of(), List.of());
    }

    private static List<Integer> cantidades(DistribucionNiveles distribucion) {
        return distribucion.niveles().stream().map(ConteoNivel::cantidad).toList();
    }

    private static List<BigDecimal> porcentajes(DistribucionNiveles distribucion) {
        return distribucion.niveles().stream().map(ConteoNivel::porcentaje).toList();
    }

    private static DistribucionNiveles distribucion(String... porcentajes) {
        List<ConteoNivel> conteos = new ArrayList<>();
        for (int i = 0; i < porcentajes.length; i++) {
            conteos.add(new ConteoNivel(NIVELES.get(i), 0, bd(porcentajes[i])));
        }
        return new DistribucionNiveles(10, conteos);
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

}
