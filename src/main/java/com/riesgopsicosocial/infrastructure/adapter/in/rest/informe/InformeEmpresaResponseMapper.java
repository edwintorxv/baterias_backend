package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe;

import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelEstres;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.*;

import java.util.List;
import java.util.function.Function;

/**
 * Traduce el informe de empresa del dominio al JSON de la API. No calcula nada: el Word y el JSON
 * salen del mismo {@link InformeEmpresa}, así que dicen lo mismo.
 */
public final class InformeEmpresaResponseMapper {

    /** Riesgo psicosocial: el nombre del catálogo de niveles. */
    private static final Function<NivelRiesgo, String> RIESGO = NivelRiesgo::nombre;
    /** Estrés: mismos niveles 1–5 con las etiquetas del manual de estrés. */
    private static final Function<NivelRiesgo, String> ESTRES = n -> NivelEstres.etiqueta(n.id());

    private InformeEmpresaResponseMapper() {
    }

    public static InformeEmpresaResponse toResponse(InformeEmpresa informe) {
        AnalisisGrupo general = informe.general();
        ConcentracionRiesgo concentracion = informe.concentracion();
        ResultadosExtralaborales extralaboral = informe.extralaboral();
        SintomasEstres sintomas = informe.sintomas();
        return new InformeEmpresaResponse(
                new ClienteInformeResponse(informe.cliente().id(), informe.cliente().nit(), informe.cliente().nombre()),
                informe.anio(),
                new CoberturaResponse(general.cobertura().esperados(), general.cobertura().evaluados(),
                        general.cobertura().porcentaje()),
                general.coberturaPorCiudad().stream()
                        .map(c -> new CoberturaCiudadResponse(c.idCiudad(), c.ciudad(), c.cobertura().esperados(),
                                c.cobertura().evaluados(), c.cobertura().porcentaje()))
                        .toList(),
                general.pendientesCalculo(),
                poblacion(informe.poblacion()),
                new ResultadosGlobalesResponse(
                        distribucion(general.intralaboral(), RIESGO),
                        distribucion(general.extralaboral(), RIESGO),
                        distribucion(general.totalGeneral(), RIESGO),
                        distribucion(general.estres(), ESTRES)),
                general.dominios().stream().map(InformeEmpresaResponseMapper::forma).toList(),
                new ConcentracionIntralaboralResponse(
                        concentracion.dominios().stream()
                                .map(d -> new DistribucionItemResponse(d.idDominio(), d.nombre(),
                                        distribucion(d.distribucion(), RIESGO)))
                                .toList(),
                        desglose(concentracion.intralaboralPorCiudad(), RIESGO),
                        desglose(concentracion.intralaboralPorArea(), RIESGO)),
                new FactoresExtralaboralesResponse(
                        dimensiones(extralaboral.dimensiones()),
                        desglose(extralaboral.porCiudad(), RIESGO),
                        desglose(extralaboral.porArea(), RIESGO)),
                new SintomasEstresResponse(
                        sintomas.personas(),
                        sintomas.dimensiones().stream().map(InformeEmpresaResponseMapper::frecuencia).toList(),
                        desglose(concentracion.estresPorCiudad(), ESTRES),
                        desglose(concentracion.estresPorArea(), ESTRES)),
                conclusiones(informe.conclusiones()));
    }

    private static ConclusionesEmpresaResponse conclusiones(ConclusionesInforme conclusiones) {
        DatosEvaluador p = conclusiones.profesional();
        return new ConclusionesEmpresaResponse(conclusiones.conclusiones(), conclusiones.accionesCortoPlazo(),
                conclusiones.accionesMedianoPlazo(), conclusiones.accionesLargoPlazo(),
                p == null ? null : new EvaluadorInformeResponse(p.id(), p.numeroIdentificacion(), p.nombre(),
                        p.profesion(), p.posgrado(), p.tarjetaProfesional(), p.licenciaSaludOcupacional(),
                        p.fechaExpedicionLicencia(), p.tieneFirma()),
                conclusiones.faltantes());
    }

    private static PoblacionResponse poblacion(DescripcionPoblacion poblacion) {
        return new PoblacionResponse(poblacion.personas(), poblacion.variables().stream()
                .map(v -> new VariablePoblacionResponse(v.nombre(), v.categorias().stream()
                        .map(c -> new ConteoCategoriaResponse(c.categoria(), c.cantidad(), c.porcentaje()))
                        .toList()))
                .toList());
    }

    private static FormaIntralaboralResponse forma(DominiosForma forma) {
        return new FormaIntralaboralResponse(forma.idCuestionario(), forma.forma(), forma.total(), forma.dominios().stream()
                .map(d -> new DominioEmpresaResponse(d.idDominioCuestionario(), d.nombre(),
                        distribucion(d.distribucion(), RIESGO), dimensiones(d.dimensiones())))
                .toList());
    }

    private static List<DistribucionItemResponse> dimensiones(List<DistribucionDimension> dimensiones) {
        return dimensiones.stream()
                .map(d -> new DistribucionItemResponse(d.idDimensionCuestionario(), d.nombre(),
                        distribucion(d.distribucion(), RIESGO)))
                .toList();
    }

    private static DesgloseGruposResponse desglose(DesgloseGrupos desglose, Function<NivelRiesgo, String> etiqueta) {
        return new DesgloseGruposResponse(
                desglose.grupos().stream()
                        .map(g -> new DistribucionGrupoResponse(g.grupo(), distribucion(g.distribucion(), etiqueta)))
                        .toList(),
                desglose.agrupados(),
                desglose.omitidos());
    }

    private static FrecuenciaDimensionResponse frecuencia(FrecuenciaDimension dimension) {
        return new FrecuenciaDimensionResponse(dimension.idDimensionCuestionario(), dimension.nombre(),
                dimension.personas(), dimension.items(), dimension.respuestas(), dimension.frecuencias().stream()
                .map(f -> new ConteoFrecuenciaResponse(f.opcion().id(), f.opcion().nombre(), f.cantidad(), f.porcentaje()))
                .toList());
    }

    private static DistribucionNivelesResponse distribucion(DistribucionNiveles distribucion,
                                                            Function<NivelRiesgo, String> etiqueta) {
        return new DistribucionNivelesResponse(
                distribucion.total(),
                distribucion.niveles().stream()
                        .map(c -> new ConteoNivelResponse(c.nivel().id(), etiqueta.apply(c.nivel()), c.cantidad(),
                                c.porcentaje()))
                        .toList());
    }

}
