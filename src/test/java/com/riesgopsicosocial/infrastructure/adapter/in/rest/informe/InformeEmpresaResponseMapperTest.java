package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InformeEmpresaResponseMapperTest {

    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo ALTO = new NivelRiesgo(4L, "Riesgo alto");
    private static final DistribucionNiveles DISTRIBUCION = new DistribucionNiveles(3, List.of(
            new ConteoNivel(BAJO, 1, new BigDecimal("33.3")), new ConteoNivel(ALTO, 2, new BigDecimal("66.7"))));

    @Test
    void traduceTodasLasSeccionesDelInforme() {
        InformeEmpresaResponse respuesta = InformeEmpresaResponseMapper.toResponse(informe());

        assertEquals("Empresa X", respuesta.cliente().nombre());
        assertEquals(2026, respuesta.anio());
        assertEquals(new CoberturaResponse(4, 3, new BigDecimal("75.0")), respuesta.cobertura());
        assertEquals(List.of(new CoberturaCiudadResponse(1L, "BOGOTA, D.C.", 4, 3, new BigDecimal("75.0"))),
                respuesta.coberturaPorCiudad());
        assertEquals(1, respuesta.pendientesCalculo());

        assertEquals(new PoblacionResponse(3, List.of(new VariablePoblacionResponse("Sexo",
                List.of(new ConteoCategoriaResponse("Femenino", 3, new BigDecimal("100.0")))))), respuesta.poblacion());

        // Resultados globales agrupados; el estrés con las etiquetas del manual de estrés
        ResultadosGlobalesResponse globales = respuesta.resultadosGlobales();
        assertEquals(List.of("Riesgo bajo", "Riesgo alto"), etiquetas(globales.intralaboral()));
        assertEquals(List.of("Riesgo bajo", "Riesgo alto"), etiquetas(globales.totalGeneral()));
        assertEquals(List.of("Bajo", "Alto"), etiquetas(globales.estres()));
        assertEquals(new ConteoNivelResponse(4L, "Riesgo alto", 2, new BigDecimal("66.7")),
                globales.extralaboral().niveles().get(1));

        FormaIntralaboralResponse forma = respuesta.formas().get(0);
        assertEquals("A", forma.forma());
        assertEquals(3, forma.personas());
        assertEquals("Recompensas", forma.dominios().get(0).nombre());
        assertEquals(List.of("Reconocimiento y compensación"),
                forma.dominios().get(0).dimensiones().stream().map(DistribucionItemResponse::nombre).toList());

        ConcentracionIntralaboralResponse concentracion = respuesta.concentracionIntralaboral();
        assertEquals(1L, concentracion.dominios().get(0).id(), "dominios con A y B juntas: id del dominio");
        assertEquals(List.of("BOGOTA, D.C.", "Otras ciudades"),
                concentracion.porCiudad().grupos().stream().map(DistribucionGrupoResponse::grupo).toList());
        assertEquals(3, concentracion.porCiudad().agrupados());
        assertEquals(1, concentracion.porArea().omitidos());

        FactoresExtralaboralesResponse extralaboral = respuesta.factoresExtralaborales();
        assertEquals(36L, extralaboral.dimensiones().get(0).id());
        assertEquals("Ventas", extralaboral.porArea().grupos().get(0).grupo());

        SintomasEstresResponse sintomas = respuesta.sintomasEstres();
        assertEquals(3, sintomas.personas());
        assertEquals(new FrecuenciaDimensionResponse(43L, "Síntomas fisiológicos", 3, 2, 6,
                        List.of(new ConteoFrecuenciaResponse(1L, "Siempre", 6, new BigDecimal("100.0")))),
                sintomas.dimensiones().get(0));
        assertEquals(List.of("Bajo", "Alto"), etiquetas(sintomas.porCiudad().grupos().get(0).distribucion()),
                "estrés por ciudad con las etiquetas del manual de estrés");

        ConclusionesEmpresaResponse conclusiones = respuesta.conclusiones();
        assertEquals("Conclusiones.", conclusiones.conclusiones());
        assertEquals("Psicóloga de Prueba", conclusiones.profesional().nombre());
        assertTrue(conclusiones.profesional().tieneFirma());
        assertEquals(List.of("acciones a mediano plazo"), conclusiones.faltantes(), "le dice al front qué falta para descargar");
    }

    private static List<String> etiquetas(DistribucionNivelesResponse distribucion) {
        return distribucion.niveles().stream().map(ConteoNivelResponse::nivel).toList();
    }

    private static InformeEmpresa informe() {
        Cobertura cobertura = new Cobertura(4, 3, new BigDecimal("75.0"));
        AnalisisGrupo general = new AnalisisGrupo(cobertura,
                List.of(new CoberturaCiudad(1L, "BOGOTA, D.C.", cobertura)), 1,
                DISTRIBUCION, DISTRIBUCION, DISTRIBUCION, DISTRIBUCION,
                List.of(new DominiosForma(1L, "A", 3, List.of(new DistribucionDominio(10L, "Recompensas", DISTRIBUCION,
                        List.of(new DistribucionDimension(100L, "Reconocimiento y compensación", DISTRIBUCION)))))));
        DesgloseGrupos ciudades = new DesgloseGrupos(List.of(new DistribucionGrupo("BOGOTA, D.C.", DISTRIBUCION),
                new DistribucionGrupo("Otras ciudades", DISTRIBUCION)), 3, 0);
        DesgloseGrupos areas = new DesgloseGrupos(List.of(new DistribucionGrupo("Ventas", DISTRIBUCION)), 0, 1);
        return new InformeEmpresa(new DatosCliente(1L, "900", "Empresa X"), 2026, general,
                new DescripcionPoblacion(3, List.of(new VariablePoblacion("Sexo",
                        List.of(new ConteoCategoria("Femenino", 3, new BigDecimal("100.0")))))),
                new ConcentracionRiesgo(List.of(new DominioConsolidado(1L, "Liderazgo", DISTRIBUCION)),
                        ciudades, areas, ciudades, areas),
                new ResultadosExtralaborales(List.of(new DistribucionDimension(36L, "Tiempo fuera del trabajo", DISTRIBUCION)),
                        ciudades, areas),
                new SintomasEstres(3, List.of(new FrecuenciaDimension(43L, "Síntomas fisiológicos", 3, 2, 6,
                        List.of(new ConteoFrecuencia(new OpcionFrecuencia(1L, "Siempre"), 6, new BigDecimal("100.0")))))),
                new ConclusionesInforme("Conclusiones.", "Corto", null, "Largo", new DatosEvaluador(1L, "52000111",
                        "Psicóloga de Prueba", "Psicóloga", null, "TP-1", "LSO-1", LocalDate.of(2020, 1, 15), true)));
    }

}
