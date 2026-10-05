package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado;

import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension.BaremoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio.BaremoDominioJpaRepository;
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
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo.NivelRiesgoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Arma la estructura de un cuestionario (dimensiones, dominios, factores, baremos) y el
 * catálogo de niveles de riesgo. Lo comparten los dos adaptadores del motor de cálculo.
 */
@Component
public class CatalogoCalculoLoader {

    private final CuestionarioJpaRepository cuestionarioRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final DominioCuestionarioJpaRepository dominioCuestionarioRepository;
    private final DimensionJpaRespository dimensionRepository;
    private final DominioJpaRepository dominioRepository;
    private final PreguntaJpaRepository preguntaRepository;
    private final BaremoDimensionJpaRepository baremoDimensionRepository;
    private final BaremoDominioJpaRepository baremoDominioRepository;
    private final BaremoCuestionarioJpaRepository baremoCuestionarioRepository;
    private final NivelRiesgoJpaRepository nivelRiesgoRepository;

    public CatalogoCalculoLoader(CuestionarioJpaRepository cuestionarioRepository,
                                DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                                DominioCuestionarioJpaRepository dominioCuestionarioRepository,
                                DimensionJpaRespository dimensionRepository,
                                DominioJpaRepository dominioRepository,
                                PreguntaJpaRepository preguntaRepository,
                                BaremoDimensionJpaRepository baremoDimensionRepository,
                                BaremoDominioJpaRepository baremoDominioRepository,
                                BaremoCuestionarioJpaRepository baremoCuestionarioRepository,
                                NivelRiesgoJpaRepository nivelRiesgoRepository) {
        this.cuestionarioRepository = cuestionarioRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.dominioCuestionarioRepository = dominioCuestionarioRepository;
        this.dimensionRepository = dimensionRepository;
        this.dominioRepository = dominioRepository;
        this.preguntaRepository = preguntaRepository;
        this.baremoDimensionRepository = baremoDimensionRepository;
        this.baremoDominioRepository = baremoDominioRepository;
        this.baremoCuestionarioRepository = baremoCuestionarioRepository;
        this.nivelRiesgoRepository = nivelRiesgoRepository;
    }

    public ConfiguracionCuestionario cargarConfiguracion(Long idCuestionario, Long idGrupoOcupacional) {
        CuestionarioEntity cuestionario = cuestionarioRepository.findById(idCuestionario)
                .orElseThrow(() -> new ResourceNotFoundException("Cuestionario no encontrado con id: " + idCuestionario));
        Map<Long, NivelRiesgo> niveles = cargarNiveles();

        List<DimensionCuestionarioEntity> dimensionesCuestionario = dimensionCuestionarioRepository
                .findByFkCuestionario(idCuestionario).stream()
                .sorted(Comparator.comparing(DimensionCuestionarioEntity::getId))
                .toList();
        List<DominioCuestionarioEntity> dominiosCuestionario = dominioCuestionarioRepository
                .findByFkCuestionario(idCuestionario).stream()
                .sorted(Comparator.comparing(DominioCuestionarioEntity::getId))
                .toList();

        Set<Long> idsDimensionCuestionario = dimensionesCuestionario.stream()
                .map(DimensionCuestionarioEntity::getId).collect(Collectors.toSet());
        Set<Long> idsDominioCuestionario = dominiosCuestionario.stream()
                .map(DominioCuestionarioEntity::getId).collect(Collectors.toSet());

        Map<Long, DimensionEntity> dimensiones = porId(dimensionRepository.findAllById(
                dimensionesCuestionario.stream().map(DimensionCuestionarioEntity::getFkDimension).toList()), DimensionEntity::getId);
        Map<Long, DominioEntity> dominios = porId(dominioRepository.findAllById(
                dominiosCuestionario.stream().map(DominioCuestionarioEntity::getFkDominio).toList()), DominioEntity::getId);
        Map<Long, Long> dominioCuestionarioPorDominio = dominiosCuestionario.stream()
                .collect(Collectors.toMap(DominioCuestionarioEntity::getFkDominio, DominioCuestionarioEntity::getId));

        Map<Long, Long> preguntasPorDimension = idsDimensionCuestionario.isEmpty() ? Map.of()
                : preguntaRepository.findByFkDimensionCuestionarioIn(idsDimensionCuestionario).stream()
                .collect(Collectors.groupingBy(PreguntaEntity::getFkDimensionCuestionario, Collectors.counting()));

        Map<Long, List<RangoBaremo>> baremosDimension = idsDimensionCuestionario.isEmpty() ? Map.of()
                : baremoDimensionRepository
                .findByFkDimensionCuestionarioInAndFkGrupoOcupacional(idsDimensionCuestionario, idGrupoOcupacional).stream()
                .collect(Collectors.groupingBy(BaremoDimensionEntity::getFkDimensionCuestionario,
                        Collectors.mapping(b -> rango(b.getValorMinimo(), b.getValorMaximo(), b.getFkNivelRiesgo(), niveles),
                                Collectors.toList())));
        Map<Long, List<RangoBaremo>> baremosDominio = idsDominioCuestionario.isEmpty() ? Map.of()
                : baremoDominioRepository
                .findByFkDominioCuestionarioInAndFkGrupoOcupacional(idsDominioCuestionario, idGrupoOcupacional).stream()
                .collect(Collectors.groupingBy(BaremoDominioEntity::getFkDominioCuestionario,
                        Collectors.mapping(b -> rango(b.getValorMinimo(), b.getValorMaximo(), b.getFkNivelRiesgo(), niveles),
                                Collectors.toList())));
        List<RangoBaremo> baremosCuestionario = baremoCuestionarioRepository
                .findByFkCuestionarioAndFkGrupoOcupacional(idCuestionario, idGrupoOcupacional).stream()
                .map(b -> rango(b.getValorMinimo(), b.getValorMaximo(), b.getFkNivelRiesgo(), niveles))
                .toList();

        List<ConfiguracionDimension> configDimensiones = dimensionesCuestionario.stream()
                .map(dc -> {
                    DimensionEntity dimension = dimensiones.get(dc.getFkDimension());
                    return new ConfiguracionDimension(
                            dc.getId(),
                            dominioCuestionarioPorDominio.get(dimension.getFkDominio()),
                            dimension.getNombre(),
                            dc.getFactorTransformacion(),
                            dc.getPeso(),
                            preguntasPorDimension.getOrDefault(dc.getId(), 0L).intValue(),
                            baremosDimension.getOrDefault(dc.getId(), List.of()));
                })
                .toList();
        List<ConfiguracionDominio> configDominios = dominiosCuestionario.stream()
                .map(dc -> new ConfiguracionDominio(
                        dc.getId(),
                        dominios.get(dc.getFkDominio()).getNombre(),
                        dc.getFactorTransformacion(),
                        baremosDominio.getOrDefault(dc.getId(), List.of())))
                .toList();

        return new ConfiguracionCuestionario(
                cuestionario.getId(),
                cuestionario.getForma(),
                cuestionario.getMetodoCalculo(),
                cuestionario.getFactorTransformacion(),
                idGrupoOcupacional,
                configDimensiones,
                configDominios,
                baremosCuestionario);
    }

    public Map<Long, NivelRiesgo> cargarNiveles() {
        return nivelRiesgoRepository.findAll().stream()
                .collect(Collectors.toMap(NivelRiesgoEntity::getId, n -> new NivelRiesgo(n.getId(), n.getNombre())));
    }

    private static RangoBaremo rango(BigDecimal min, BigDecimal max, Long idNivel,
                                     Map<Long, NivelRiesgo> niveles) {
        return new RangoBaremo(min, max, niveles.get(idNivel));
    }

    private static <T> Map<Long, T> porId(Iterable<T> entidades, Function<T, Long> id) {
        Map<Long, T> mapa = new HashMap<>();
        entidades.forEach(e -> mapa.put(id.apply(e), e));
        return mapa;
    }

}
