package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informe;

import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.domain.model.informe.DatosAplicacion;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluado;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.grupoocupacional.GrupoOcupacionalEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.grupoocupacional.GrupoOcupacionalJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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

    public InformeEvaluadoPersistenceAdapter(ClienteJpaRepository clienteRepository,
                                             EvaluadoJpaRepository evaluadoRepository,
                                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                                             AplicacionJpaRepository aplicacionRepository,
                                             GrupoOcupacionalJpaRepository grupoOcupacionalRepository) {
        this.clienteRepository = clienteRepository;
        this.evaluadoRepository = evaluadoRepository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.grupoOcupacionalRepository = grupoOcupacionalRepository;
    }

    @Override
    public Optional<DatosCliente> buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .map(c -> new DatosCliente(c.getId(), c.getNit(), c.getNombre()));
    }

    @Override
    public Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado) {
        return evaluadoRepository.findById(idEvaluado)
                .map(e -> new DatosEvaluado(e.getId(), e.getNumeroIdentificacion(), e.getNombre(), e.getApellido()));
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

        return aplicacionRepository.findByFkEvaluadoClienteIn(relaciones.keySet()).stream()
                .map(a -> {
                    EvaluadoClienteEntity relacion = relaciones.get(a.getFkEvaluadoCliente());
                    return new DatosAplicacion(a.getId(), a.getFechaAplicacion(), relacion.getNombreCargo(),
                            relacion.getNombreArea(), a.getFkGrupoOcupacional(), grupos.get(a.getFkGrupoOcupacional()));
                })
                .toList();
    }

}
