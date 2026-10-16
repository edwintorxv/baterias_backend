package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informe;

import com.riesgopsicosocial.application.port.out.informe.InformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.ConclusionesInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.AplicacionEmpresa;
import com.riesgopsicosocial.domain.model.informe.empresa.CiudadTrabajo;
import com.riesgopsicosocial.domain.model.informe.empresa.CuestionarioInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.DimensionInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.DominioInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.FichaPersona;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.PersonaCargada;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDimension;
import com.riesgopsicosocial.domain.model.resultado.ResultadoDominio;
import com.riesgopsicosocial.domain.model.resultado.ResultadoTotalGeneral;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioJpaResository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionJpaRespository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa.InformeEmpresaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado.CatalogoCalculoLoader;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario.ResultadoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario.ResultadoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodimension.ResultadoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodimension.ResultadoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio.ResultadoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio.ResultadoDominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadototalgeneral.ResultadoTotalGeneralEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadototalgeneral.ResultadoTotalGeneralJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga en bloque (pocas consultas para toda la empresa) lo que necesita el informe de empresa.
 */
@Component
public class InformeEmpresaPersistenceAdapter implements InformeEmpresaPort {

    private final ClienteJpaRepository clienteRepository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final AplicacionJpaRepository aplicacionRepository;
    private final RespuestaJpaRepository respuestaRepository;
    private final ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository;
    private final ResultadoDominioJpaRepository resultadoDominioRepository;
    private final ResultadoDimensionJpaRepository resultadoDimensionRepository;
    private final ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository;
    private final CuestionarioJpaRepository cuestionarioRepository;
    private final DominioCuestionarioJpaRepository dominioCuestionarioRepository;
    private final DominioJpaRepository dominioRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final DimensionJpaRespository dimensionRepository;
    private final CiudadMunicipioJpaResository ciudadRepository;
    private final OpcionRespuestaJpaRepository opcionRespuestaRepository;
    private final FichasPersonaLoader fichasPersona;
    private final InformeEmpresaJpaRepository informeEmpresaRepository;
    private final EvaluadorJpaRepository evaluadorRepository;
    private final CatalogoCalculoLoader catalogo;

    public InformeEmpresaPersistenceAdapter(ClienteJpaRepository clienteRepository,
                                            EvaluadoClienteJpaRepository evaluadoClienteRepository,
                                            AplicacionJpaRepository aplicacionRepository,
                                            RespuestaJpaRepository respuestaRepository,
                                            ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository,
                                            ResultadoDominioJpaRepository resultadoDominioRepository,
                                            ResultadoDimensionJpaRepository resultadoDimensionRepository,
                                            ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository,
                                            CuestionarioJpaRepository cuestionarioRepository,
                                            DominioCuestionarioJpaRepository dominioCuestionarioRepository,
                                            DominioJpaRepository dominioRepository,
                                            DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                                            DimensionJpaRespository dimensionRepository,
                                            CiudadMunicipioJpaResository ciudadRepository,
                                            OpcionRespuestaJpaRepository opcionRespuestaRepository,
                                            FichasPersonaLoader fichasPersona,
                                            InformeEmpresaJpaRepository informeEmpresaRepository,
                                            EvaluadorJpaRepository evaluadorRepository,
                                            CatalogoCalculoLoader catalogo) {
        this.clienteRepository = clienteRepository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.respuestaRepository = respuestaRepository;
        this.resultadoCuestionarioRepository = resultadoCuestionarioRepository;
        this.resultadoDominioRepository = resultadoDominioRepository;
        this.resultadoDimensionRepository = resultadoDimensionRepository;
        this.resultadoTotalGeneralRepository = resultadoTotalGeneralRepository;
        this.cuestionarioRepository = cuestionarioRepository;
        this.dominioCuestionarioRepository = dominioCuestionarioRepository;
        this.dominioRepository = dominioRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.dimensionRepository = dimensionRepository;
        this.ciudadRepository = ciudadRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.fichasPersona = fichasPersona;
        this.informeEmpresaRepository = informeEmpresaRepository;
        this.evaluadorRepository = evaluadorRepository;
        this.catalogo = catalogo;
    }

    @Override
    public Optional<DatosCliente> buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .map(c -> new DatosCliente(c.getId(), c.getNit(), c.getNombre()));
    }

    @Override
    public List<PersonaCargada> buscarPersonasCargadas(Long idCliente) {
        List<EvaluadoClienteEntity> relaciones = evaluadoClienteRepository.findByFkClienteAndActivoTrue(idCliente);
        Map<Long, String> ciudades = ciudadRepository.findAllById(relaciones.stream()
                        .map(EvaluadoClienteEntity::getFkCiudadTrabajo).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CiudadMunicipioEntity::getId, CiudadMunicipioEntity::getNombre));
        Map<Long, FichaPersona> fichas = fichasPersona.cargar(relaciones);
        return relaciones.stream()
                .map(ec -> new PersonaCargada(ec.getId(), ec.getNombreArea(), ec.getFkCiudadTrabajo() == null ? null
                        : new CiudadTrabajo(ec.getFkCiudadTrabajo(), ciudades.get(ec.getFkCiudadTrabajo())),
                        fichas.get(ec.getId())))
                .toList();
    }

    @Override
    public List<AplicacionEmpresa> buscarAplicaciones(Long idCliente, int anio) {
        Set<Long> relaciones = idsRelaciones(idCliente);
        if (relaciones.isEmpty()) {
            return List.of();
        }
        List<AplicacionEntity> aplicaciones = aplicacionRepository.buscarPorRelacionesYPeriodo(relaciones,
                LocalDateTime.of(anio, 1, 1, 0, 0), LocalDateTime.of(anio + 1, 1, 1, 0, 0));
        if (aplicaciones.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = aplicaciones.stream().map(AplicacionEntity::getId).collect(Collectors.toSet());
        Set<Long> conRespuestas = respuestaRepository.buscarAplicacionesConRespuestas(ids);
        Map<Long, ResultadoAplicacion> resultados = cargarResultados(aplicaciones);

        return aplicaciones.stream()
                .map(a -> new AplicacionEmpresa(a.getId(), a.getFkEvaluadoCliente(), a.getFechaAplicacion(),
                        conRespuestas.contains(a.getId()), resultados.get(a.getId()),
                        new FiltrosAplicacion(a.getAtiendeClientes(), a.getEsJefe())))
                .toList();
    }

    @Override
    public List<Integer> buscarAnios(Long idCliente) {
        Set<Long> relaciones = idsRelaciones(idCliente);
        if (relaciones.isEmpty()) {
            return List.of();
        }
        return aplicacionRepository.findByFkEvaluadoClienteIn(relaciones).stream()
                .map(a -> a.getFechaAplicacion().getYear())
                .distinct()
                .toList();
    }

    /**
     * Cuestionarios con sus dominios y, dentro de cada dominio, sus dimensiones (en el orden de
     * sus ids). Una dimensión pertenece al dominio del mismo cuestionario que tiene su
     * {@code dimension.fk_dominio}.
     */
    @Override
    public List<CuestionarioInforme> buscarCuestionarios() {
        Map<Long, String> nombresDominio = dominioRepository.findAll().stream()
                .collect(Collectors.toMap(DominioEntity::getId, DominioEntity::getNombre));
        Map<Long, DimensionEntity> dimensiones = dimensionRepository.findAll().stream()
                .collect(Collectors.toMap(DimensionEntity::getId, Function.identity()));
        // (cuestionario, dominio) → dimensiones del cuestionario en ese dominio
        Map<List<Long>, List<DimensionInforme>> dimensionesPorDominio = dimensionCuestionarioRepository.findAll().stream()
                .sorted(Comparator.comparing(DimensionCuestionarioEntity::getId))
                .collect(Collectors.groupingBy(
                        dc -> List.of(dc.getFkCuestionario(), dimensiones.get(dc.getFkDimension()).getFkDominio()),
                        Collectors.mapping(dc -> new DimensionInforme(dc.getId(),
                                        dimensiones.get(dc.getFkDimension()).getNombre(), dc.getCondicionAplicacion()),
                                Collectors.toList())));
        Map<Long, List<DominioInforme>> dominiosPorCuestionario = dominioCuestionarioRepository.findAll().stream()
                .sorted(Comparator.comparing(DominioCuestionarioEntity::getId))
                .collect(Collectors.groupingBy(DominioCuestionarioEntity::getFkCuestionario,
                        Collectors.mapping(dc -> new DominioInforme(dc.getId(), dc.getFkDominio(), nombresDominio.get(dc.getFkDominio()),
                                        dimensionesPorDominio.getOrDefault(
                                                List.of(dc.getFkCuestionario(), dc.getFkDominio()), List.of())),
                                Collectors.toList())));

        return cuestionarioRepository.findAll().stream()
                .sorted(Comparator.comparing(CuestionarioEntity::getId))
                .map(c -> new CuestionarioInforme(c.getId(), c.getForma(), c.getMetodoCalculo(),
                        dominiosPorCuestionario.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Override
    public List<NivelRiesgo> buscarNiveles() {
        return catalogo.cargarNiveles().values().stream()
                .sorted(Comparator.comparing(NivelRiesgo::id))
                .toList();
    }

    @Override
    public List<RespuestaSintoma> buscarRespuestasEstres(Collection<Long> idsAplicacion) {
        if (idsAplicacion.isEmpty()) {
            return List.of();
        }
        return respuestaRepository.buscarPorMetodoCalculo(idsAplicacion, MetodoCalculo.PROMEDIO_PONDERADO).stream()
                .map(r -> new RespuestaSintoma(r.getFkAplicacion(), r.getFkPregunta(), r.getFkDimensionCuestionario(),
                        r.getFkOpcionRespuesta()))
                .toList();
    }

    @Override
    public List<OpcionFrecuencia> buscarOpcionesEstres() {
        return opcionRespuestaRepository.buscarPorMetodoCalculo(MetodoCalculo.PROMEDIO_PONDERADO).stream()
                .map(o -> new OpcionFrecuencia(o.getId(), o.getNombre()))
                .toList();
    }

    @Override
    public Optional<ConclusionesInforme> buscarConclusiones(Long idCliente, int anio) {
        return informeEmpresaRepository.findByFkClienteAndAnio(idCliente, anio)
                .map(i -> new ConclusionesInforme(i.getConclusiones(), i.getAccionesCortoPlazo(),
                        i.getAccionesMedianoPlazo(), i.getAccionesLargoPlazo(),
                        i.getFkEvaluador() == null ? null : evaluadorRepository.findById(i.getFkEvaluador())
                                .map(e -> new DatosEvaluador(e.getId(), e.getNumeroIdentificacion(), e.getNombre(),
                                        e.getProfesion(), e.getPosgrado(), e.getTarjetaProfesional(),
                                        e.getLicenciaSaludOcupacional(), e.getFechaExpedicionLicencia(),
                                        e.getFirma() != null))
                                .orElse(null)));
    }

    @Override
    public Optional<FirmaEvaluador> buscarFirma(Long idEvaluador) {
        return evaluadorRepository.findById(idEvaluador)
                .filter(e -> e.getFirma() != null)
                .map(e -> new FirmaEvaluador(e.getFirma(), e.getFirmaTipoContenido()));
    }

    private Set<Long> idsRelaciones(Long idCliente) {
        return evaluadoClienteRepository.findByFkCliente(idCliente).stream()
                .map(EvaluadoClienteEntity::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Resultados por aplicación con cuestionarios, dominios y dimensiones de dominio (A/B), en
     * tres consultas. Los nombres y la forma salen del catálogo de cuestionarios. Las
     * dimensiones de C no se cargan: el informe aún no las usa.
     */
    private Map<Long, ResultadoAplicacion> cargarResultados(List<AplicacionEntity> aplicaciones) {
        Set<Long> ids = aplicaciones.stream().map(AplicacionEntity::getId).collect(Collectors.toSet());
        Map<Long, List<ResultadoCuestionarioEntity>> cuestionarios = resultadoCuestionarioRepository.findByFkAplicacionIn(ids)
                .stream().collect(Collectors.groupingBy(ResultadoCuestionarioEntity::getFkAplicacion));
        if (cuestionarios.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ResultadoDominioEntity>> dominios = resultadoDominioRepository.findByFkAplicacionIn(cuestionarios.keySet())
                .stream().collect(Collectors.groupingBy(ResultadoDominioEntity::getFkAplicacion));
        Map<Long, List<ResultadoDimensionEntity>> dimensiones = resultadoDimensionRepository
                .findByFkAplicacionIn(cuestionarios.keySet())
                .stream().collect(Collectors.groupingBy(ResultadoDimensionEntity::getFkAplicacion));
        Map<Long, ResultadoTotalGeneralEntity> totalesGenerales = resultadoTotalGeneralRepository
                .findByFkAplicacionIn(cuestionarios.keySet()).stream()
                .collect(Collectors.toMap(ResultadoTotalGeneralEntity::getFkAplicacion, Function.identity()));
        Map<Long, CuestionarioInforme> configuracion = buscarCuestionarios().stream()
                .collect(Collectors.toMap(CuestionarioInforme::idCuestionario, Function.identity()));
        Map<Long, NivelRiesgo> niveles = catalogo.cargarNiveles();

        Map<Long, ResultadoAplicacion> resultados = new HashMap<>();
        for (AplicacionEntity aplicacion : aplicaciones) {
            List<ResultadoCuestionarioEntity> suyos = cuestionarios.get(aplicacion.getId());
            if (suyos == null) {
                continue;
            }
            Map<Long, ResultadoDominioEntity> suyosDominios = dominios.getOrDefault(aplicacion.getId(), List.of()).stream()
                    .collect(Collectors.toMap(ResultadoDominioEntity::getFkDominioCuestionario, Function.identity()));
            Map<Long, ResultadoDimensionEntity> suyasDimensiones = dimensiones.getOrDefault(aplicacion.getId(), List.of())
                    .stream()
                    .collect(Collectors.toMap(ResultadoDimensionEntity::getFkDimensionCuestionario, Function.identity()));
            List<ResultadoCuestionario> resultadoCuestionarios = suyos.stream()
                    .sorted(Comparator.comparing(ResultadoCuestionarioEntity::getFkCuestionario))
                    .map(rc -> {
                        CuestionarioInforme config = configuracion.get(rc.getFkCuestionario());
                        List<ResultadoDominio> resultadoDominios = config.dominios().stream()
                                .filter(d -> suyosDominios.containsKey(d.idDominioCuestionario()))
                                .map(d -> {
                                    ResultadoDominioEntity e = suyosDominios.get(d.idDominioCuestionario());
                                    return new ResultadoDominio(d.idDominioCuestionario(), d.nombre(), e.getPuntajeBruto(),
                                            e.getPuntajeTransformado(), niveles.get(e.getFkNivelRiesgo()));
                                })
                                .toList();
                        List<ResultadoDimension> resultadoDimensiones = config.dominios().stream()
                                .flatMap(d -> d.dimensiones().stream()
                                        .filter(dim -> suyasDimensiones.containsKey(dim.idDimensionCuestionario()))
                                        .map(dim -> {
                                            ResultadoDimensionEntity e = suyasDimensiones.get(dim.idDimensionCuestionario());
                                            return new ResultadoDimension(dim.idDimensionCuestionario(),
                                                    d.idDominioCuestionario(), dim.nombre(), e.getPuntajeBruto(),
                                                    e.getPuntajeTransformado(), niveles.get(e.getFkNivelRiesgo()));
                                        }))
                                .toList();
                        return new ResultadoCuestionario(rc.getFkCuestionario(), config.forma(), rc.getPuntajeBruto(),
                                rc.getPuntajeTransformado(), niveles.get(rc.getFkNivelRiesgo()), resultadoDominios,
                                resultadoDimensiones);
                    })
                    .toList();
            ResultadoTotalGeneralEntity total = totalesGenerales.get(aplicacion.getId());
            ResultadoTotalGeneral totalGeneral = total == null ? null : new ResultadoTotalGeneral(
                    total.getFkCuestionarioIntralaboral(), configuracion.get(total.getFkCuestionarioIntralaboral()).forma(),
                    total.getPuntajeBruto(), total.getPuntajeTransformado(), niveles.get(total.getFkNivelRiesgo()));
            resultados.put(aplicacion.getId(), new ResultadoAplicacion(aplicacion.getId(),
                    aplicacion.getFkGrupoOcupacional(), suyos.get(0).getFechaCalculo(), resultadoCuestionarios,
                    totalGeneral));
        }
        return resultados;
    }

}
