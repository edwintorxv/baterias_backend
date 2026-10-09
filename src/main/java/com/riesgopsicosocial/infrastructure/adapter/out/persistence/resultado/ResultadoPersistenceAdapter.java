package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado;

import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario.ResultadoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario.ResultadoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio.ResultadoDominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio.ResultadoDominioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodimension.ResultadoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodimension.ResultadoDimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadototalgeneral.ResultadoTotalGeneralEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadototalgeneral.ResultadoTotalGeneralJpaRepository;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResultadoPersistenceAdapter implements ResultadoPort {

    private final ResultadoDimensionJpaRepository resultadoDimensionRepository;
    private final ResultadoDominioJpaRepository resultadoDominioRepository;
    private final ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository;
    private final ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository;
    private final CatalogoCalculoLoader catalogo;

    public ResultadoPersistenceAdapter(ResultadoDimensionJpaRepository resultadoDimensionRepository,
                                       ResultadoDominioJpaRepository resultadoDominioRepository,
                                       ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository,
                                       ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository,
                                       CatalogoCalculoLoader catalogo) {
        this.resultadoDimensionRepository = resultadoDimensionRepository;
        this.resultadoDominioRepository = resultadoDominioRepository;
        this.resultadoCuestionarioRepository = resultadoCuestionarioRepository;
        this.resultadoTotalGeneralRepository = resultadoTotalGeneralRepository;
        this.catalogo = catalogo;
    }

    @Override
    public void reemplazar(ResultadoAplicacion resultado) {
        Long idAplicacion = resultado.idAplicacion();
        LocalDateTime fecha = resultado.fechaCalculo();

        resultadoDimensionRepository.eliminarPorAplicacion(idAplicacion);
        resultadoDominioRepository.eliminarPorAplicacion(idAplicacion);
        resultadoCuestionarioRepository.eliminarPorAplicacion(idAplicacion);
        resultadoTotalGeneralRepository.eliminarPorAplicacion(idAplicacion);

        List<ResultadoDimensionEntity> dimensiones = new ArrayList<>();
        List<ResultadoDominioEntity> dominios = new ArrayList<>();
        List<ResultadoCuestionarioEntity> cuestionarios = new ArrayList<>();

        for (ResultadoCuestionario rc : resultado.cuestionarios()) {
            cuestionarios.add(new ResultadoCuestionarioEntity(null, idAplicacion, rc.idCuestionario(),
                    rc.puntajeBruto(), rc.puntajeTransformado(), rc.nivelRiesgo().id(), fecha));
            rc.dominios().forEach(rd -> dominios.add(new ResultadoDominioEntity(null, idAplicacion,
                    rd.idDominioCuestionario(), rd.puntajeBruto(), rd.puntajeTransformado(), rd.nivelRiesgo().id(), fecha)));
            rc.dimensiones().forEach(rd -> dimensiones.add(new ResultadoDimensionEntity(null, idAplicacion,
                    rd.idDimensionCuestionario(), rd.puntajeBruto(), rd.puntajeTransformado(), rd.nivelRiesgo().id(), fecha)));
        }

        resultadoCuestionarioRepository.saveAll(cuestionarios);
        resultadoDominioRepository.saveAll(dominios);
        resultadoDimensionRepository.saveAll(dimensiones);

        ResultadoTotalGeneral total = resultado.totalGeneral();
        if (total != null) {
            resultadoTotalGeneralRepository.save(new ResultadoTotalGeneralEntity(null, idAplicacion,
                    total.idCuestionarioIntralaboral(), total.puntajeBruto(), total.puntajeTransformado(),
                    total.nivelRiesgo().id(), fecha));
        }
    }

    @Override
    public ResultadoAplicacion buscarPorAplicacion(Long idAplicacion, Long idGrupoOcupacional) {
        List<ResultadoCuestionarioEntity> cuestionarios = resultadoCuestionarioRepository.findByFkAplicacion(idAplicacion)
                .stream().sorted(Comparator.comparing(ResultadoCuestionarioEntity::getFkCuestionario)).toList();
        if (cuestionarios.isEmpty()) {
            return new ResultadoAplicacion(idAplicacion, idGrupoOcupacional, null, List.of(), null);
        }

        Map<Long, ResultadoDominioEntity> dominios = resultadoDominioRepository.findByFkAplicacion(idAplicacion).stream()
                .collect(Collectors.toMap(ResultadoDominioEntity::getFkDominioCuestionario, r -> r));
        Map<Long, ResultadoDimensionEntity> dimensiones = resultadoDimensionRepository.findByFkAplicacion(idAplicacion).stream()
                .collect(Collectors.toMap(ResultadoDimensionEntity::getFkDimensionCuestionario, r -> r));
        Map<Long, NivelRiesgo> niveles = catalogo.cargarNiveles();

        // La configuración aporta forma, nombres y el orden de dominios/dimensiones de cada cuestionario.
        List<ResultadoCuestionario> resultado = cuestionarios.stream()
                .map(rc -> {
                    ConfiguracionCuestionario config = catalogo.cargarConfiguracion(rc.getFkCuestionario(), idGrupoOcupacional);
                    List<ResultadoDominio> suyosDominios = config.dominios().stream()
                            .filter(d -> dominios.containsKey(d.idDominioCuestionario()))
                            .map(d -> {
                                ResultadoDominioEntity e = dominios.get(d.idDominioCuestionario());
                                return new ResultadoDominio(d.idDominioCuestionario(), d.nombre(), e.getPuntajeBruto(),
                                        e.getPuntajeTransformado(), niveles.get(e.getFkNivelRiesgo()));
                            })
                            .toList();
                    List<ResultadoDimension> suyasDimensiones = config.dimensiones().stream()
                            .filter(d -> dimensiones.containsKey(d.idDimensionCuestionario()))
                            .map(d -> {
                                ResultadoDimensionEntity e = dimensiones.get(d.idDimensionCuestionario());
                                return new ResultadoDimension(d.idDimensionCuestionario(), d.idDominioCuestionario(),
                                        d.nombre(), e.getPuntajeBruto(), e.getPuntajeTransformado(),
                                        niveles.get(e.getFkNivelRiesgo()));
                            })
                            .toList();
                    return new ResultadoCuestionario(rc.getFkCuestionario(), config.forma(), rc.getPuntajeBruto(),
                            rc.getPuntajeTransformado(), niveles.get(rc.getFkNivelRiesgo()), suyosDominios, suyasDimensiones);
                })
                .toList();

        // La forma intralaboral se toma de su propio resultado, que siempre existe si hay total general.
        ResultadoTotalGeneral totalGeneral = resultadoTotalGeneralRepository.findByFkAplicacion(idAplicacion)
                .map(t -> new ResultadoTotalGeneral(t.getFkCuestionarioIntralaboral(),
                        resultado.stream()
                                .filter(rc -> rc.idCuestionario().equals(t.getFkCuestionarioIntralaboral()))
                                .map(ResultadoCuestionario::forma)
                                .findFirst().orElse(null),
                        t.getPuntajeBruto(), t.getPuntajeTransformado(), niveles.get(t.getFkNivelRiesgo())))
                .orElse(null);

        return new ResultadoAplicacion(idAplicacion, idGrupoOcupacional, cuestionarios.get(0).getFechaCalculo(),
                resultado, totalGeneral);
    }

}
