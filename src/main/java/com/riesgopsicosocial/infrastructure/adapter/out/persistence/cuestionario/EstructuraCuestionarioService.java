package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremototalgeneral.BaremoTotalGeneralEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremototalgeneral.BaremoTotalGeneralJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.EstructuraCuestionario.Baremo;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionJpaRespository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario.DominioCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala.EscalaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Consulta de la configuración de los cuestionarios (pantalla Configuración → Cuestionarios), en solo lectura:
 * cambiarla es cosa de una migración. Ordena como el manual: dominios y dimensiones por su id (se cargaron en
 * el orden de las tablas del manual) y preguntas por número.
 */
@Service
@Transactional(readOnly = true)
public class EstructuraCuestionarioService {

    private static final Comparator<Baremo> ORDEN_BAREMO =
            Comparator.comparing(Baremo::idGrupoOcupacional).thenComparing(Baremo::idNivelRiesgo);

    private final CuestionarioJpaRepository cuestionarioRepository;
    private final DominioCuestionarioJpaRepository dominioCuestionarioRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final DominioJpaRepository dominioRepository;
    private final DimensionJpaRespository dimensionRepository;
    private final PreguntaJpaRepository preguntaRepository;
    private final EscalaJpaRepository escalaRepository;
    private final EscalaDetalleJpaRepository escalaDetalleRepository;
    private final OpcionRespuestaJpaRepository opcionRespuestaRepository;
    private final NivelRiesgoJpaRepository nivelRiesgoRepository;
    private final BaremoCuestionarioJpaRepository baremoCuestionarioRepository;
    private final BaremoDominioJpaRepository baremoDominioRepository;
    private final BaremoDimensionJpaRepository baremoDimensionRepository;
    private final BaremoTotalGeneralJpaRepository baremoTotalGeneralRepository;

    public EstructuraCuestionarioService(CuestionarioJpaRepository cuestionarioRepository,
                                         DominioCuestionarioJpaRepository dominioCuestionarioRepository,
                                         DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                                         DominioJpaRepository dominioRepository,
                                         DimensionJpaRespository dimensionRepository,
                                         PreguntaJpaRepository preguntaRepository,
                                         EscalaJpaRepository escalaRepository,
                                         EscalaDetalleJpaRepository escalaDetalleRepository,
                                         OpcionRespuestaJpaRepository opcionRespuestaRepository,
                                         NivelRiesgoJpaRepository nivelRiesgoRepository,
                                         BaremoCuestionarioJpaRepository baremoCuestionarioRepository,
                                         BaremoDominioJpaRepository baremoDominioRepository,
                                         BaremoDimensionJpaRepository baremoDimensionRepository,
                                         BaremoTotalGeneralJpaRepository baremoTotalGeneralRepository) {
        this.cuestionarioRepository = cuestionarioRepository;
        this.dominioCuestionarioRepository = dominioCuestionarioRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.dominioRepository = dominioRepository;
        this.dimensionRepository = dimensionRepository;
        this.preguntaRepository = preguntaRepository;
        this.escalaRepository = escalaRepository;
        this.escalaDetalleRepository = escalaDetalleRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.nivelRiesgoRepository = nivelRiesgoRepository;
        this.baremoCuestionarioRepository = baremoCuestionarioRepository;
        this.baremoDominioRepository = baremoDominioRepository;
        this.baremoDimensionRepository = baremoDimensionRepository;
        this.baremoTotalGeneralRepository = baremoTotalGeneralRepository;
    }

    public EstructuraCuestionario estructura(Long idCuestionario) {
        CuestionarioEntity cuestionario = cuestionarioRepository.findById(idCuestionario)
                .orElseThrow(() -> new ResourceNotFoundException("Cuestionario no encontrado con id: " + idCuestionario));
        Map<Long, String> niveles = nivelesRiesgo();

        List<DominioCuestionarioEntity> dominiosCuestionario = dominioCuestionarioRepository.findByFkCuestionario(idCuestionario);
        List<DimensionCuestionarioEntity> dimensionesCuestionario = dimensionCuestionarioRepository.findByFkCuestionario(idCuestionario);
        Set<Long> idsDominioCuestionario = dominiosCuestionario.stream()
                .map(DominioCuestionarioEntity::getId).collect(Collectors.toSet());
        Set<Long> idsDimensionCuestionario = dimensionesCuestionario.stream()
                .map(DimensionCuestionarioEntity::getId).collect(Collectors.toSet());
        List<PreguntaEntity> preguntas = idsDimensionCuestionario.isEmpty() ? List.of()
                : preguntaRepository.findByFkDimensionCuestionarioIn(idsDimensionCuestionario);

        Map<Long, DimensionEntity> dimensiones = dimensionRepository.findAll().stream()
                .collect(Collectors.toMap(DimensionEntity::getId, d -> d));
        Map<Long, String> nombresDominio = dominioRepository.findAll().stream()
                .collect(Collectors.toMap(DominioEntity::getId, DominioEntity::getNombre));

        Map<Long, List<Baremo>> baremosDominio = idsDominioCuestionario.isEmpty() ? Map.of()
                : baremoDominioRepository.findByFkDominioCuestionarioIn(idsDominioCuestionario).stream()
                .collect(Collectors.groupingBy(BaremoDominioEntity::getFkDominioCuestionario, Collectors.mapping(b ->
                        baremo(b.getFkGrupoOcupacional(), b.getFkNivelRiesgo(), b.getValorMinimo(), b.getValorMaximo(), niveles),
                        Collectors.toList())));
        Map<Long, List<Baremo>> baremosDimension = idsDimensionCuestionario.isEmpty() ? Map.of()
                : baremoDimensionRepository.findByFkDimensionCuestionarioIn(idsDimensionCuestionario).stream()
                .collect(Collectors.groupingBy(BaremoDimensionEntity::getFkDimensionCuestionario, Collectors.mapping(b ->
                        baremo(b.getFkGrupoOcupacional(), b.getFkNivelRiesgo(), b.getValorMinimo(), b.getValorMaximo(), niveles),
                        Collectors.toList())));
        Map<Long, List<EstructuraCuestionario.Pregunta>> preguntasPorDimension = preguntas.stream()
                .sorted(Comparator.comparing(PreguntaEntity::getNumero))
                .collect(Collectors.groupingBy(PreguntaEntity::getFkDimensionCuestionario, Collectors.mapping(p ->
                        new EstructuraCuestionario.Pregunta(p.getId(), p.getNumero(), p.getDescripcion(), p.getFkEscala()),
                        Collectors.toList())));

        // Cada dimensión va en el dominio al que pertenece (dimension.fk_dominio).
        Map<Long, List<EstructuraCuestionario.Dimension>> dimensionesPorDominio = dimensionesCuestionario.stream()
                .sorted(Comparator.comparing(DimensionCuestionarioEntity::getFkDimension))
                .collect(Collectors.groupingBy(dc -> dimensiones.get(dc.getFkDimension()).getFkDominio(),
                        Collectors.mapping(dc -> new EstructuraCuestionario.Dimension(dc.getId(),
                                dimensiones.get(dc.getFkDimension()).getNombre(), dc.getFactorTransformacion(), dc.getPeso(),
                                dc.getCondicionAplicacion(), dc.getMaxItemsSinRespuesta(),
                                ordenados(baremosDimension.get(dc.getId())),
                                preguntasPorDimension.getOrDefault(dc.getId(), List.of())), Collectors.toList())));

        List<EstructuraCuestionario.Dominio> dominios = dominiosCuestionario.stream()
                .sorted(Comparator.comparing(DominioCuestionarioEntity::getFkDominio))
                .map(dc -> new EstructuraCuestionario.Dominio(dc.getId(), nombresDominio.get(dc.getFkDominio()),
                        dc.getFactorTransformacion(), ordenados(baremosDominio.get(dc.getId())),
                        dimensionesPorDominio.getOrDefault(dc.getFkDominio(), List.of())))
                .toList();

        List<Baremo> baremosCuestionario = baremoCuestionarioRepository.findByFkCuestionario(idCuestionario).stream()
                .map(b -> baremo(b.getFkGrupoOcupacional(), b.getFkNivelRiesgo(), b.getValorMinimo(), b.getValorMaximo(), niveles))
                .sorted(ORDEN_BAREMO)
                .toList();

        return new EstructuraCuestionario(cuestionario.getId(), cuestionario.getForma(), cuestionario.getNombre(),
                cuestionario.getMetodoCalculo(), cuestionario.getFactorTransformacion(), preguntas.size(),
                baremosCuestionario, dominios,
                escalas(preguntas.stream().map(PreguntaEntity::getFkEscala).collect(Collectors.toSet())));
    }

    /**
     * Baremos del total general, uno por cuestionario intralaboral (A y B). El grupo ocupacional de cada uno
     * sale de los baremos de su cuestionario (A solo grupo 1, B solo grupo 2).
     */
    public List<EstructuraCuestionario.TotalGeneral> totalGeneral() {
        Map<Long, String> niveles = nivelesRiesgo();
        List<CuestionarioEntity> cuestionarios = cuestionarioRepository.findAll();
        Optional<CuestionarioEntity> extralaboral = cuestionarios.stream()
                .filter(c -> c.getMetodoCalculo() == MetodoCalculo.SUMA_DIRECTA)
                .findFirst();
        Map<Long, List<BaremoTotalGeneralEntity>> porIntralaboral = baremoTotalGeneralRepository.findAll().stream()
                .collect(Collectors.groupingBy(BaremoTotalGeneralEntity::getFkCuestionarioIntralaboral));

        return cuestionarios.stream()
                .filter(c -> c.getMetodoCalculo() == MetodoCalculo.SUMA_POR_DOMINIOS)
                .sorted(Comparator.comparing(CuestionarioEntity::getId))
                .map(intralaboral -> {
                    Long grupo = baremoCuestionarioRepository.findByFkCuestionario(intralaboral.getId()).stream()
                            .map(BaremoCuestionarioEntity::getFkGrupoOcupacional)
                            .findFirst().orElse(null);
                    List<Baremo> baremos = porIntralaboral.getOrDefault(intralaboral.getId(), List.of()).stream()
                            .map(b -> baremo(grupo, b.getFkNivelRiesgo(), b.getValorMinimo(), b.getValorMaximo(), niveles))
                            .sorted(Comparator.comparing(Baremo::idNivelRiesgo))
                            .toList();
                    return new EstructuraCuestionario.TotalGeneral(intralaboral.getId(), intralaboral.getForma(),
                            extralaboral.map(CuestionarioEntity::getForma).orElse(null),
                            extralaboral.map(e -> intralaboral.getFactorTransformacion().add(e.getFactorTransformacion()))
                                    .orElse(null),
                            baremos);
                })
                .toList();
    }

    /** Las escalas usadas, cada una con sus opciones en el orden de la escala y el puntaje de cada una. */
    private List<EstructuraCuestionario.Escala> escalas(Set<Long> idsEscala) {
        if (idsEscala.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombresOpcion = opcionRespuestaRepository.findAll().stream()
                .collect(Collectors.toMap(OpcionRespuestaEntity::getId, OpcionRespuestaEntity::getNombre));
        Map<Long, List<EstructuraCuestionario.Opcion>> opciones = escalaDetalleRepository.findAll().stream()
                .filter(e -> idsEscala.contains(e.getFkEscala()))
                .sorted(Comparator.comparing(EscalaDetalleEntity::getFkOpcionRespuesta))
                .collect(Collectors.groupingBy(EscalaDetalleEntity::getFkEscala, Collectors.mapping(e ->
                        new EstructuraCuestionario.Opcion(e.getFkOpcionRespuesta(), nombresOpcion.get(e.getFkOpcionRespuesta()),
                                e.getValor()), Collectors.toList())));
        return escalaRepository.findAllById(idsEscala).stream()
                .sorted(Comparator.comparing(EscalaEntity::getId))
                .map(e -> new EstructuraCuestionario.Escala(e.getId(), e.getNombre(), opciones.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    private Map<Long, String> nivelesRiesgo() {
        return nivelRiesgoRepository.findAll().stream()
                .collect(Collectors.toMap(NivelRiesgoEntity::getId, NivelRiesgoEntity::getNombre));
    }

    private static Baremo baremo(Long grupo, Long nivel, BigDecimal minimo, BigDecimal maximo, Map<Long, String> niveles) {
        return new Baremo(grupo, nivel, niveles.get(nivel), minimo, maximo);
    }

    private static List<Baremo> ordenados(List<Baremo> baremos) {
        return baremos == null ? List.of() : baremos.stream().sorted(ORDEN_BAREMO).toList();
    }

}
