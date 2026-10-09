package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informe;

import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.domain.model.informe.DatosAplicacion;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluado;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.grupoocupacional.GrupoOcupacionalEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.grupoocupacional.GrupoOcupacionalJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InformeEvaluadoPersistenceAdapter implements InformeEvaluadoPort {

    private final ClienteJpaRepository clienteRepository;
    private final EvaluadoJpaRepository evaluadoRepository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final AplicacionJpaRepository aplicacionRepository;
    private final GrupoOcupacionalJpaRepository grupoOcupacionalRepository;
    private final SexoJpaRepository sexoRepository;
    private final EvaluadorJpaRepository evaluadorRepository;

    public InformeEvaluadoPersistenceAdapter(ClienteJpaRepository clienteRepository,
                                             EvaluadoJpaRepository evaluadoRepository,
                                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                                             AplicacionJpaRepository aplicacionRepository,
                                             GrupoOcupacionalJpaRepository grupoOcupacionalRepository,
                                             SexoJpaRepository sexoRepository,
                                             EvaluadorJpaRepository evaluadorRepository) {
        this.clienteRepository = clienteRepository;
        this.evaluadoRepository = evaluadoRepository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.grupoOcupacionalRepository = grupoOcupacionalRepository;
        this.sexoRepository = sexoRepository;
        this.evaluadorRepository = evaluadorRepository;
    }

    @Override
    public Optional<DatosCliente> buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .map(c -> new DatosCliente(c.getId(), c.getNit(), c.getNombre()));
    }

    @Override
    public Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado) {
        return evaluadoRepository.findById(idEvaluado)
                .map(e -> new DatosEvaluado(e.getId(), e.getNumeroIdentificacion(), e.getNombre(), e.getApellido(),
                        e.getFkSexo() == null ? null : sexoRepository.findById(e.getFkSexo()).map(SexoEntity::getNombre).orElse(null),
                        e.getAnioNacimiento()));
    }

    @Override
    public boolean existeRelacion(Long idCliente, Long idEvaluado) {
        return !evaluadoClienteRepository.findByFkEvaluadoAndFkCliente(idEvaluado, idCliente).isEmpty();
    }

    @Override
    public List<DatosAplicacion> buscarAplicaciones(Long idCliente, Long idEvaluado) {
        // Puede haber varias relaciones con el mismo cliente (recontratación): se incluyen todas.
        Map<Long, EvaluadoClienteEntity> relaciones = evaluadoClienteRepository
                .findByFkEvaluadoAndFkCliente(idEvaluado, idCliente).stream()
                .collect(Collectors.toMap(EvaluadoClienteEntity::getId, Function.identity()));
        if (relaciones.isEmpty()) {
            return List.of();
        }
        Map<Long, String> grupos = grupoOcupacionalRepository.findAll().stream()
                .collect(Collectors.toMap(GrupoOcupacionalEntity::getId, GrupoOcupacionalEntity::getNombre));

        List<AplicacionEntity> aplicaciones = aplicacionRepository.findByFkEvaluadoClienteIn(relaciones.keySet());
        Map<Long, DatosEvaluador> evaluadores = evaluadorRepository.findAllById(aplicaciones.stream()
                        .map(AplicacionEntity::getFkEvaluador).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(EvaluadorEntity::getId, InformeEvaluadoPersistenceAdapter::toDatosEvaluador));

        return aplicaciones.stream()
                .map(a -> {
                    EvaluadoClienteEntity relacion = relaciones.get(a.getFkEvaluadoCliente());
                    return new DatosAplicacion(a.getId(), a.getFechaAplicacion(), relacion.getNombreCargo(),
                            relacion.getNombreArea(), a.getFkGrupoOcupacional(), grupos.get(a.getFkGrupoOcupacional()),
                            a.getFkEvaluador() == null ? null : evaluadores.get(a.getFkEvaluador()),
                            a.getObservaciones(), a.getRecomendaciones());
                })
                .toList();
    }

    @Override
    public Optional<FirmaEvaluador> buscarFirma(Long idEvaluador) {
        return evaluadorRepository.findById(idEvaluador)
                .filter(e -> e.getFirma() != null)
                .map(e -> new FirmaEvaluador(e.getFirma(), e.getFirmaTipoContenido()));
    }

    private static DatosEvaluador toDatosEvaluador(EvaluadorEntity e) {
        return new DatosEvaluador(e.getId(), e.getNumeroIdentificacion(), e.getNombre(), e.getProfesion(),
                e.getPosgrado(), e.getTarjetaProfesional(), e.getLicenciaSaludOcupacional(),
                e.getFechaExpedicionLicencia(), e.getFirma() != null);
    }

}
