package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conclusiones y plan de acción del informe de empresa: una fila por cliente y año, que se crea o
 * reemplaza completa con cada guardado.
 */
@Service
public class ConclusionesInformeEmpresaService {

    private final InformeEmpresaJpaRepository repository;
    private final ClienteJpaRepository clienteRepository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final AplicacionJpaRepository aplicacionRepository;
    private final EvaluadorJpaRepository evaluadorRepository;

    public ConclusionesInformeEmpresaService(InformeEmpresaJpaRepository repository,
                                             ClienteJpaRepository clienteRepository,
                                             EvaluadoClienteJpaRepository evaluadoClienteRepository,
                                             AplicacionJpaRepository aplicacionRepository,
                                             EvaluadorJpaRepository evaluadorRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.evaluadorRepository = evaluadorRepository;
    }

    /** Lo guardado del año; si no hay nada, una fila vacía sin id (no se guarda). */
    @Transactional(readOnly = true)
    public InformeEmpresaEntity consultar(Long idCliente, int anio) {
        validarInformeExiste(idCliente, anio);
        return repository.findByFkClienteAndAnio(idCliente, anio).orElseGet(() -> vacio(idCliente, anio));
    }

    /**
     * Crea o reemplaza los textos y el profesional del año. Textos en blanco = sin texto. El
     * evaluador se valida activo solo al asignarlo o cambiarlo: si después se retira, el informe
     * conserva el suyo (como en las aplicaciones).
     */
    @Transactional
    public InformeEmpresaEntity guardar(Long idCliente, int anio, InformeEmpresaEntity datos) {
        validarInformeExiste(idCliente, anio);
        InformeEmpresaEntity informe = repository.findByFkClienteAndAnio(idCliente, anio)
                .orElseGet(() -> vacio(idCliente, anio));
        if (datos.getFkEvaluador() != null && !datos.getFkEvaluador().equals(informe.getFkEvaluador())) {
            validarEvaluadorActivo(datos.getFkEvaluador());
        }
        informe.setConclusiones(texto(datos.getConclusiones()));
        informe.setAccionesCortoPlazo(texto(datos.getAccionesCortoPlazo()));
        informe.setAccionesMedianoPlazo(texto(datos.getAccionesMedianoPlazo()));
        informe.setAccionesLargoPlazo(texto(datos.getAccionesLargoPlazo()));
        informe.setFkEvaluador(datos.getFkEvaluador());
        if (informe.getId() != null) {
            informe.setFechaModificado(LocalDateTime.now());
            informe.setUsuarioModifica(UsuarioActual.nombre());
        } else {
            informe.setUsuarioCrea(UsuarioActual.nombre());
        }
        return repository.save(informe);
    }

    /** Mismas reglas que el informe: el cliente existe y tiene aplicaciones en el año. */
    private void validarInformeExiste(Long idCliente, int anio) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
        }
        Set<Long> relaciones = evaluadoClienteRepository.findByFkCliente(idCliente).stream()
                .map(EvaluadoClienteEntity::getId)
                .collect(Collectors.toSet());
        if (relaciones.isEmpty() || aplicacionRepository.buscarPorRelacionesYPeriodo(relaciones,
                LocalDateTime.of(anio, 1, 1, 0, 0), LocalDateTime.of(anio + 1, 1, 1, 0, 0)).isEmpty()) {
            throw new ResourceNotFoundException("El cliente " + idCliente + " no tiene aplicaciones en el año " + anio);
        }
    }

    private void validarEvaluadorActivo(Long fkEvaluador) {
        EvaluadorEntity evaluador = evaluadorRepository.findById(fkEvaluador)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluador no encontrado con id: " + fkEvaluador));
        if (!Boolean.TRUE.equals(evaluador.getActivo())) {
            throw new BusinessException("El evaluador " + fkEvaluador + " está inactivo y no puede asignarse al informe");
        }
    }

    private static InformeEmpresaEntity vacio(Long idCliente, int anio) {
        InformeEmpresaEntity informe = new InformeEmpresaEntity();
        informe.setFkCliente(idCliente);
        informe.setAnio(anio);
        return informe;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

}
