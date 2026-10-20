package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado.BorradoResultados;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AplicacionService {

    private static final String ESTADO_DEFAULT = "FINALIZADA";

    private final AplicacionJpaRepository repository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final TipoCargoJpaRepository tipoCargoRepository;
    private final EvaluadorJpaRepository evaluadorRepository;
    private final BorradoResultados borradoResultados;

    public AplicacionService(AplicacionJpaRepository repository,
                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                             TipoCargoJpaRepository tipoCargoRepository,
                             EvaluadorJpaRepository evaluadorRepository,
                             BorradoResultados borradoResultados) {
        this.repository = repository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.tipoCargoRepository = tipoCargoRepository;
        this.evaluadorRepository = evaluadorRepository;
        this.borradoResultados = borradoResultados;
    }

    /** Listado general de las aplicaciones de esos clientes (del año, si viene). */
    @Transactional(readOnly = true)
    public List<AplicacionListadoFila> listado(Collection<Long> fksCliente, Integer anio) {
        return fksCliente.isEmpty() ? List.of() : repository.listado(fksCliente, anio);
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
        entidad.setUsuarioCrea(UsuarioActual.nombre());
        return repository.save(entidad);
    }

    /**
     * Si cambia algo de lo que salen los resultados (grupo ocupacional o preguntas filtro), se borran: la
     * aplicación queda pendiente de cálculo.
     */
    @Transactional
    public AplicacionEntity actualizar(Long id, AplicacionEntity datos) {
        AplicacionEntity existente = obtenerPorId(id);
        List<Object> antes = baseDelCalculo(existente);

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

        if (!antes.equals(baseDelCalculo(existente))) {
            borradoResultados.borrar(id);
        }

        existente.setFechaModificado(LocalDateTime.now());
        existente.setUsuarioModifica(UsuarioActual.nombre());
        return repository.save(existente);
    }

    /** Lo de la aplicación de lo que salen los resultados (además de las respuestas). */
    private static List<Object> baseDelCalculo(AplicacionEntity aplicacion) {
        return Arrays.asList(aplicacion.getFkGrupoOcupacional(), aplicacion.getAtiendeClientes(), aplicacion.getEsJefe());
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
