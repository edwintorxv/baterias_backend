package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Usuarios de la API. Valida aquí lo que la BD también ataja (vínculo según el rol, usuario único),
 * para responder con un mensaje claro en vez del error genérico de integridad.
 */
@Service
public class UsuarioService {

    private final UsuarioJpaRepository repository;
    private final UsuarioClienteJpaRepository usuarioClienteRepository;
    private final EvaluadorJpaRepository evaluadorRepository;
    private final ClienteJpaRepository clienteRepository;
    private final EvaluadoJpaRepository evaluadoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioJpaRepository repository, UsuarioClienteJpaRepository usuarioClienteRepository,
                          EvaluadorJpaRepository evaluadorRepository, ClienteJpaRepository clienteRepository,
                          EvaluadoJpaRepository evaluadoRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.usuarioClienteRepository = usuarioClienteRepository;
        this.evaluadorRepository = evaluadorRepository;
        this.clienteRepository = clienteRepository;
        this.evaluadoRepository = evaluadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioEntity crear(UsuarioEntity entidad, String contrasena) {
        entidad.setUsuario(entidad.getUsuario().trim());
        if (repository.existsByUsuarioIgnoreCase(entidad.getUsuario())) {
            throw new BusinessException("Ya existe un usuario: " + entidad.getUsuario());
        }
        validarVinculo(entidad, null);
        entidad.setContrasenaHash(passwordEncoder.encode(contrasena));
        if (entidad.getActivo() == null) {
            entidad.setActivo(true);
        }
        return repository.save(entidad);
    }

    /**
     * La contraseña no se toca aquí: tiene su propio endpoint. Si deja de ser psicólogo, pierde sus
     * clientes asignados.
     */
    @Transactional
    public UsuarioEntity actualizar(Long id, UsuarioEntity datos) {
        UsuarioEntity existente = obtenerPorId(id);
        String usuario = datos.getUsuario().trim();
        if (!existente.getUsuario().equalsIgnoreCase(usuario) && repository.existsByUsuarioIgnoreCase(usuario)) {
            throw new BusinessException("Ya existe un usuario: " + usuario);
        }
        validarVinculo(datos, existente);
        if (existente.getRol() == Rol.PSICOLOGO && datos.getRol() != Rol.PSICOLOGO) {
            usuarioClienteRepository.borrarDeUsuario(id);
        }

        existente.setUsuario(usuario);
        existente.setNombre(datos.getNombre());
        existente.setRol(datos.getRol());
        existente.setFkEvaluador(datos.getFkEvaluador());
        existente.setFkCliente(datos.getFkCliente());
        existente.setFkEvaluado(datos.getFkEvaluado());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }
        existente.setFechaModificado(LocalDateTime.now());
        return repository.save(existente);
    }

    /** Restablece la contraseña (la escoge el administrador). */
    @Transactional
    public UsuarioEntity cambiarContrasena(Long id, String contrasena) {
        UsuarioEntity existente = obtenerPorId(id);
        existente.setContrasenaHash(passwordEncoder.encode(contrasena));
        existente.setFechaModificado(LocalDateTime.now());
        return repository.save(existente);
    }

    /** Reemplaza los clientes asignados al psicólogo por estos (vacío = ninguno). */
    @Transactional
    public List<Long> asignarClientes(Long id, Collection<Long> idsCliente) {
        UsuarioEntity usuario = obtenerPorId(id);
        if (usuario.getRol() != Rol.PSICOLOGO) {
            throw new BusinessException("Solo se asignan clientes a usuarios con rol PSICOLOGO; el usuario "
                    + id + " es " + usuario.getRol());
        }
        Set<Long> clientes = new TreeSet<>(idsCliente);
        List<Long> inexistentes = clientes.stream().filter(c -> !clienteRepository.existsById(c)).toList();
        if (!inexistentes.isEmpty()) {
            throw new ResourceNotFoundException("Clientes no encontrados con id: " + inexistentes);
        }
        usuarioClienteRepository.borrarDeUsuario(id);
        usuarioClienteRepository.saveAll(clientes.stream().map(c -> new UsuarioClienteEntity(id, c)).toList());
        return List.copyOf(clientes);
    }

    public List<Long> listarClientes(Long id) {
        obtenerPorId(id);
        return usuarioClienteRepository.findByFkUsuario(id).stream()
                .map(UsuarioClienteEntity::getFkCliente)
                .sorted()
                .toList();
    }

    public UsuarioEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    public UsuarioEntity obtenerPorUsuario(String usuario) {
        return repository.findByUsuarioIgnoreCase(usuario.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuario));
    }

    public List<UsuarioEntity> listar(Rol rol, Boolean activo) {
        if (rol != null && activo != null) {
            return repository.findByRolAndActivo(rol, activo);
        }
        if (rol != null) {
            return repository.findByRol(rol);
        }
        return activo != null ? repository.findByActivo(activo) : repository.findAll();
    }

    /**
     * Cada rol con lo suyo: PSICOLOGO con un evaluador activo que no tenga ya usuario, CLIENTE con
     * su empresa, EVALUADO con un evaluado que no tenga ya usuario, ADMIN sin vínculo.
     *
     * @param existente usuario que se actualiza (para no chocar consigo mismo); null al crear
     */
    private void validarVinculo(UsuarioEntity datos, UsuarioEntity existente) {
        Rol rol = datos.getRol();
        Map<String, Long> vinculos = new LinkedHashMap<>();
        vinculos.put("idEvaluador", datos.getFkEvaluador());
        vinculos.put("idCliente", datos.getFkCliente());
        vinculos.put("idEvaluado", datos.getFkEvaluado());
        String requerido = switch (rol) {
            case ADMIN -> null;
            case PSICOLOGO -> "idEvaluador";
            case CLIENTE -> "idCliente";
            case EVALUADO -> "idEvaluado";
        };
        List<String> errores = new ArrayList<>();
        vinculos.forEach((campo, valor) -> {
            if (campo.equals(requerido) && valor == null) {
                errores.add(campo + " es obligatorio para el rol " + rol);
            } else if (!campo.equals(requerido) && valor != null) {
                errores.add(campo + " no aplica para el rol " + rol);
            }
        });
        if (!errores.isEmpty()) {
            throw new ValidationException("El vínculo no corresponde al rol " + rol, errores);
        }

        switch (rol) {
            case PSICOLOGO -> {
                Long idEvaluador = datos.getFkEvaluador();
                boolean mismo = existente != null && idEvaluador.equals(existente.getFkEvaluador());
                if (!mismo) {
                    EvaluadorEntity evaluador = evaluadorRepository.findById(idEvaluador)
                            .orElseThrow(() -> new ResourceNotFoundException("Evaluador no encontrado con id: " + idEvaluador));
                    if (!Boolean.TRUE.equals(evaluador.getActivo())) {
                        throw new BusinessException("El evaluador " + idEvaluador + " está inactivo");
                    }
                    if (repository.existsByFkEvaluador(idEvaluador)) {
                        throw new BusinessException("El evaluador " + idEvaluador + " ya tiene un usuario");
                    }
                }
            }
            case CLIENTE -> {
                if (!clienteRepository.existsById(datos.getFkCliente())) {
                    throw new ResourceNotFoundException("Cliente no encontrado con id: " + datos.getFkCliente());
                }
            }
            case EVALUADO -> {
                Long idEvaluado = datos.getFkEvaluado();
                boolean mismo = existente != null && idEvaluado.equals(existente.getFkEvaluado());
                if (!mismo) {
                    if (!evaluadoRepository.existsById(idEvaluado)) {
                        throw new ResourceNotFoundException("Evaluado no encontrado con id: " + idEvaluado);
                    }
                    if (repository.existsByFkEvaluado(idEvaluado)) {
                        throw new BusinessException("El evaluado " + idEvaluado + " ya tiene un usuario");
                    }
                }
            }
            case ADMIN -> {
            }
        }
    }

}
