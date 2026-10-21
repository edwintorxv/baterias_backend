package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteJpaRepository;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Qué clientes ve quien hace la petición: el ADMIN todos (solo entra donde lo dejan los permisos por
 * rol), el PSICOLOGO sus asignados, el CLIENTE el suyo y el EVALUADO ninguno. Lo que no ve responde
 * 404 con el mismo mensaje que si no existiera, para no revelar que existe.
 */
@Component
public class AlcanceUsuario {

    private final UsuarioClienteJpaRepository usuarioClienteRepository;
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository;
    private final AplicacionJpaRepository aplicacionRepository;
    private final RespuestaJpaRepository respuestaRepository;

    public AlcanceUsuario(UsuarioClienteJpaRepository usuarioClienteRepository,
                          EvaluadoClienteJpaRepository evaluadoClienteRepository,
                          AplicacionJpaRepository aplicacionRepository, RespuestaJpaRepository respuestaRepository) {
        this.usuarioClienteRepository = usuarioClienteRepository;
        this.evaluadoClienteRepository = evaluadoClienteRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.respuestaRepository = respuestaRepository;
    }

    public Optional<UsuarioAutenticado> actual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                ? Optional.of(usuario) : Optional.empty();
    }

    /** Ids de los clientes que ve; {@link Optional#empty()} = todos (ADMIN). */
    public Optional<Set<Long>> clientesVisibles() {
        UsuarioAutenticado usuario = actual().orElse(null);
        if (usuario == null) {
            return Optional.of(Set.of());
        }
        return switch (usuario.rol()) {
            case ADMIN -> Optional.empty();
            case PSICOLOGO -> Optional.of(usuarioClienteRepository.findByFkUsuario(usuario.id()).stream()
                    .map(UsuarioClienteEntity::getFkCliente)
                    .collect(Collectors.toUnmodifiableSet()));
            case CLIENTE -> Optional.of(Set.of(usuario.idCliente()));
            case EVALUADO -> Optional.of(Set.of());
        };
    }

    public boolean puedeVerCliente(Long idCliente) {
        return clientesVisibles().map(visibles -> visibles.contains(idCliente)).orElse(true);
    }

    public void exigirCliente(Long idCliente) {
        if (!puedeVerCliente(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
        }
    }

    /** Ids de las relaciones evaluado-cliente de sus clientes; {@link Optional#empty()} = todas (ADMIN). */
    public Optional<Set<Long>> relacionesVisibles() {
        return clientesVisibles().map(clientes -> clientes.isEmpty() ? Set.of()
                : evaluadoClienteRepository.findByFkClienteIn(clientes).stream()
                .map(EvaluadoClienteEntity::getId)
                .collect(Collectors.toUnmodifiableSet()));
    }

    /*
     * exigirRelacion / exigirAplicacion / exigirRespuesta: si el recurso no existe no hacen nada (el
     * servicio responde su propio 404 o la BD rechaza la referencia); si existe pero es de un cliente
     * que no ve, 404 con el mismo mensaje que si no existiera.
     */

    public void exigirRelacion(Long idEvaluadoCliente) {
        if (clientesVisibles().isEmpty() || idEvaluadoCliente == null) {
            return;
        }
        evaluadoClienteRepository.findById(idEvaluadoCliente)
                .filter(relacion -> !puedeVerCliente(relacion.getFkCliente()))
                .ifPresent(relacion -> {
                    throw new ResourceNotFoundException("Relación evaluado-cliente no encontrada con id: " + idEvaluadoCliente);
                });
    }

    public void exigirAplicacion(Long idAplicacion) {
        if (clientesVisibles().isEmpty() || idAplicacion == null) {
            return;
        }
        aplicacionRepository.findById(idAplicacion)
                .filter(aplicacion -> !puedeVerRelacion(aplicacion))
                .ifPresent(aplicacion -> {
                    throw new ResourceNotFoundException("Aplicación no encontrada con id: " + idAplicacion);
                });
    }

    public void exigirRespuesta(Long idRespuesta) {
        if (clientesVisibles().isEmpty() || idRespuesta == null) {
            return;
        }
        respuestaRepository.findById(idRespuesta)
                .map(RespuestaEntity::getFkAplicacion)
                .flatMap(aplicacionRepository::findById)
                .filter(aplicacion -> !puedeVerRelacion(aplicacion))
                .ifPresent(aplicacion -> {
                    throw new ResourceNotFoundException("Respuesta no encontrada con id: " + idRespuesta);
                });
    }

    /**
     * Ids de los evaluados con relación a alguno de sus clientes; {@link Optional#empty()} = todos
     * (ADMIN). Los evaluados aún sin ninguna relación no salen aquí, pero sí se pueden abrir
     * ({@link #exigirEvaluado}) para vincularlos.
     */
    public Optional<Set<Long>> evaluadosVisibles() {
        return clientesVisibles().map(clientes -> clientes.isEmpty() ? Set.of()
                : evaluadoClienteRepository.findByFkClienteIn(clientes).stream()
                .map(EvaluadoClienteEntity::getFkEvaluado)
                .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Un evaluado es una persona que puede estar en varias empresas: se ve si tiene relación con
     * alguno de sus clientes, o si todavía no tiene ninguna (recién creado, sin empresa).
     */
    public void exigirEvaluado(Long idEvaluado) {
        if (clientesVisibles().isEmpty() || idEvaluado == null) {
            return;
        }
        List<EvaluadoClienteEntity> relaciones = evaluadoClienteRepository.findByFkEvaluado(idEvaluado);
        if (!relaciones.isEmpty() && relaciones.stream().noneMatch(r -> puedeVerCliente(r.getFkCliente()))) {
            throw new ResourceNotFoundException("Evaluado no encontrado con id: " + idEvaluado);
        }
    }

    /**
     * El psicólogo solo se pone a sí mismo como evaluador (quien firma): admite vacío, el suyo o el
     * que ya tenía el registro (lo asignado antes a otro se conserva). 403 si es otro.
     *
     * @param actual evaluador que ya tiene el registro; null si es nuevo o no tenía
     */
    public void exigirFirmante(Long nuevo, Long actual) {
        UsuarioAutenticado usuario = actual().orElseThrow(() -> new AccessDeniedException("Sin sesión"));
        if (usuario.rol() == Rol.PSICOLOGO && nuevo != null && !nuevo.equals(usuario.idEvaluador())
                && !nuevo.equals(actual)) {
            throw new AccessDeniedException("Solo puede asignarse a sí mismo como evaluador");
        }
    }

    private boolean puedeVerRelacion(AplicacionEntity aplicacion) {
        return evaluadoClienteRepository.findById(aplicacion.getFkEvaluadoCliente())
                .map(relacion -> puedeVerCliente(relacion.getFkCliente()))
                .orElse(false);
    }

    /** Un evaluador y su firma los ven (y la firma la suben) el ADMIN o el propio psicólogo (403 si es otro). */
    public void exigirEvaluadorPropio(Long idEvaluador) {
        UsuarioAutenticado usuario = actual().orElseThrow(() -> new AccessDeniedException("Sin sesión"));
        if (usuario.rol() != Rol.ADMIN
                && !idEvaluador.equals(usuario.idEvaluador())) {
            throw new AccessDeniedException("Solo el propio evaluador o el administrador");
        }
    }

}
