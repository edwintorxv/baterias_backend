package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
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
    private final EvaluadorJpaRepository evaluadorRepository;

    public AplicacionService(AplicacionJpaRepository repository,
                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                             TipoCargoJpaRepository tipoCargoRepository,
                             EvaluadorJpaRepository evaluadorRepository) {
        this.repository = repository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.tipoCargoRepository = tipoCargoRepository;
        this.evaluadorRepository = evaluadorRepository;
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
        if (entidad.getFkEvaluador() != null) {
            validarEvaluadorActivo(entidad.getFkEvaluador());
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
        existente.setRecomendaciones(datos.getRecomendaciones());
        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            existente.setEstado(datos.getEstado());
        }
        if (datos.getAtiendeClientes() != null) {
            existente.setAtiendeClientes(datos.getAtiendeClientes());
        }
        if (datos.getEsJefe() != null) {
            existente.setEsJefe(datos.getEsJefe());
        }
        // Solo se valida al cambiarlo: si el evaluador se retiró después, la aplicación conserva el suyo.
        if (datos.getFkEvaluador() != null && !datos.getFkEvaluador().equals(existente.getFkEvaluador())) {
            validarEvaluadorActivo(datos.getFkEvaluador());
            existente.setFkEvaluador(datos.getFkEvaluador());
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

    /** Un evaluador retirado (activo = false) no puede recibir aplicaciones nuevas. */
    private void validarEvaluadorActivo(Long fkEvaluador) {
        EvaluadorEntity evaluador = evaluadorRepository.findById(fkEvaluador)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluador no encontrado con id: " + fkEvaluador));
        if (!Boolean.TRUE.equals(evaluador.getActivo())) {
            throw new BusinessException("El evaluador " + fkEvaluador + " está inactivo y no puede asignarse a una aplicación");
        }
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
