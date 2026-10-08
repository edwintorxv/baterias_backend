package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AplicacionService {

    private static final String ESTADO_DEFAULT = "FINALIZADA";

    private final AplicacionJpaRepository repository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final TipoCargoJpaRepository tipoCargoRepository;

    public AplicacionService(AplicacionJpaRepository repository,
                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                             TipoCargoJpaRepository tipoCargoRepository) {
        this.repository = repository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.tipoCargoRepository = tipoCargoRepository;
    }

    public AplicacionEntity crear(AplicacionEntity entidad) {
        if (entidad.getFechaAplicacion() == null) {
            entidad.setFechaAplicacion(LocalDateTime.now());
        }
        if (entidad.getEstado() == null || entidad.getEstado().isBlank()) {
            entidad.setEstado(ESTADO_DEFAULT);
        }
        if (entidad.getFkGrupoOcupacional() == null) {
            entidad.setFkGrupoOcupacional(resolverGrupoOcupacional(entidad.getFkEvaluadoCliente()));
        }
        return repository.save(entidad);
    }

    public AplicacionEntity actualizar(Long id, AplicacionEntity datos) {
        AplicacionEntity existente = obtenerPorId(id);

        boolean cambioEvaluadoCliente = !existente.getFkEvaluadoCliente().equals(datos.getFkEvaluadoCliente());
        existente.setFkEvaluadoCliente(datos.getFkEvaluadoCliente());
        if (datos.getFkGrupoOcupacional() != null) {
            existente.setFkGrupoOcupacional(datos.getFkGrupoOcupacional());
        } else if (cambioEvaluadoCliente) {
            existente.setFkGrupoOcupacional(resolverGrupoOcupacional(datos.getFkEvaluadoCliente()));
        }
        if (datos.getFechaAplicacion() != null) {
            existente.setFechaAplicacion(datos.getFechaAplicacion());
        }
        existente.setObservaciones(datos.getObservaciones());
        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            existente.setEstado(datos.getEstado());
        }
        if (datos.getAtiendeClientes() != null) {
            existente.setAtiendeClientes(datos.getAtiendeClientes());
        }
        if (datos.getEsJefe() != null) {
            existente.setEsJefe(datos.getEsJefe());
        }

        return repository.save(existente);
    }

    public AplicacionEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada con id: " + id));
    }

    public List<AplicacionEntity> listarTodos() {
        return repository.findAll();
    }

    public List<AplicacionEntity> listarPorEvaluadoCliente(Long fkEvaluadoCliente) {
        return repository.findByFkEvaluadoCliente(fkEvaluadoCliente);
    }

    public List<AplicacionEntity> listarPorEstado(String estado) {
        return repository.findByEstado(estado);
    }

    /**
     * Toma el grupo ocupacional del tipo de cargo actual del evaluado. Se guarda en la
     * aplicación como foto del momento, para que un cambio de cargo posterior no altere
     * los baremos con los que se calculan resultados históricos.
     */
    private Long resolverGrupoOcupacional(Long fkEvaluadoCliente) {
        EvaluadoClienteEntity evaluadoCliente = evaluadoClienteRepository.findById(fkEvaluadoCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relación evaluado-cliente no encontrada con id: " + fkEvaluadoCliente));

        if (evaluadoCliente.getFkTipoCargo() == null) {
            throw new BusinessException(
                    "El evaluado no tiene tipo de cargo registrado; envíe fkGrupoOcupacional o complete el tipo de cargo");
        }

        TipoCargoEntity tipoCargo = tipoCargoRepository.findById(evaluadoCliente.getFkTipoCargo())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de cargo no encontrado con id: " + evaluadoCliente.getFkTipoCargo()));

        return tipoCargo.getFkGrupoOcupacional();
    }

}
