package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.infrastructure.security.UsuarioAutenticado;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private final UsuarioJpaRepository repository = mock(UsuarioJpaRepository.class);
    private final UsuarioClienteJpaRepository usuarioClienteRepository = mock(UsuarioClienteJpaRepository.class);
    private final EvaluadorJpaRepository evaluadorRepository = mock(EvaluadorJpaRepository.class);
    private final ClienteJpaRepository clienteRepository = mock(ClienteJpaRepository.class);
    private final EvaluadoJpaRepository evaluadoRepository = mock(EvaluadoJpaRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UsuarioService service = new UsuarioService(repository, usuarioClienteRepository,
            evaluadorRepository, clienteRepository, evaluadoRepository, passwordEncoder);

    @BeforeEach
    void guardarDevuelveLoMismo() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static UsuarioEntity usuario(String correo, Rol rol) {
        UsuarioEntity entidad = new UsuarioEntity();
        entidad.setCorreo(correo);
        entidad.setNombre("Nombre");
        entidad.setRol(rol);
        return entidad;
    }

    private static EvaluadorEntity evaluador(boolean activo) {
        EvaluadorEntity evaluador = new EvaluadorEntity();
        evaluador.setActivo(activo);
        return evaluador;
    }

    @Test
    void crear_guardaLaContrasenaCifradaYActivoPorDefecto() {
        UsuarioEntity creado = service.crear(usuario("  Admin@Prueba.CO ", Rol.ADMIN), "secreta123");

        assertEquals("admin@prueba.co", creado.getCorreo(), "en minúsculas y sin espacios");
        assertNotEquals("secreta123", creado.getContrasenaHash());
        assertTrue(passwordEncoder.matches("secreta123", creado.getContrasenaHash()));
        assertTrue(creado.getActivo());
    }

    @Test
    void crear_correoRepetidoSinDistinguirMayusculas_409() {
        when(repository.existsByCorreo("admin@prueba.co")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crear(usuario("ADMIN@prueba.co", Rol.ADMIN), "secreta123"));
        verify(repository, never()).save(any());
    }

    @Test
    void crear_sinFormatoDeCorreo_400() {
        for (String correo : new String[]{"admin", "admin@prueba", "ad min@prueba.co", "@prueba.co"}) {
            assertThrows(ValidationException.class, () -> service.crear(usuario(correo, Rol.ADMIN), "secreta123"), correo);
        }
        verify(repository, never()).save(any());
    }

    @Test
    void crear_vinculoQueNoCorrespondeAlRol_400ConCadaError() {
        UsuarioEntity psicologo = usuario("psico@prueba.co", Rol.PSICOLOGO);
        psicologo.setFkCliente(1L);

        ValidationException error = assertThrows(ValidationException.class, () -> service.crear(psicologo, "secreta123"));
        assertEquals(List.of("idEvaluador es obligatorio para el rol PSICOLOGO", "idCliente no aplica para el rol PSICOLOGO"),
                error.getDetails());
    }

    @Test
    void crear_psicologo_exigeEvaluadorActivoYSinOtroUsuario() {
        UsuarioEntity psicologo = usuario("psico@prueba.co", Rol.PSICOLOGO);
        psicologo.setFkEvaluador(1L);

        when(evaluadorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crear(psicologo, "secreta123"));

        when(evaluadorRepository.findById(1L)).thenReturn(Optional.of(evaluador(false)));
        assertThrows(BusinessException.class, () -> service.crear(psicologo, "secreta123"), "inactivo");

        when(evaluadorRepository.findById(1L)).thenReturn(Optional.of(evaluador(true)));
        when(repository.existsByFkEvaluador(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.crear(psicologo, "secreta123"), "ya tiene usuario");

        when(repository.existsByFkEvaluador(1L)).thenReturn(false);
        assertEquals(1L, service.crear(psicologo, "secreta123").getFkEvaluador());
    }

    @Test
    void actualizar_mantenerSuEvaluador_noChocaConsigoMismo() {
        UsuarioEntity existente = usuario("psico@prueba.co", Rol.PSICOLOGO);
        existente.setId(5L);
        existente.setFkEvaluador(1L);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        UsuarioEntity datos = usuario("psico@prueba.co", Rol.PSICOLOGO);
        datos.setFkEvaluador(1L);
        datos.setNombre("Nuevo nombre");

        assertEquals("Nuevo nombre", service.actualizar(5L, datos).getNombre());
        verify(repository, never()).existsByFkEvaluador(any());
    }

    @Test
    void actualizar_dejaDeSerPsicologo_pierdeSusClientes() {
        UsuarioEntity existente = usuario("psico@prueba.co", Rol.PSICOLOGO);
        existente.setId(5L);
        existente.setFkEvaluador(1L);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        UsuarioEntity actualizado = service.actualizar(5L, usuario("psico@prueba.co", Rol.ADMIN));

        assertEquals(Rol.ADMIN, actualizado.getRol());
        assertNull(actualizado.getFkEvaluador());
        verify(usuarioClienteRepository).borrarDeUsuario(5L);
    }

    @Test
    void actualizar_aSiMismo_noSeCambiaElRolNiSeInactiva() {
        UsuarioEntity yo = usuario("admin@prueba.co", Rol.ADMIN);
        yo.setId(1L);
        yo.setActivo(true);
        when(repository.findById(1L)).thenReturn(Optional.of(yo));
        when(repository.countByRolAndActivo(Rol.ADMIN, true)).thenReturn(3L);
        iniciarSesion(1L);
        try {
            UsuarioEntity inactivo = usuario("admin@prueba.co", Rol.ADMIN);
            inactivo.setActivo(false);
            assertThrows(BusinessException.class, () -> service.actualizar(1L, inactivo));
            UsuarioEntity otroRol = usuario("admin@prueba.co", Rol.CLIENTE);
            otroRol.setFkCliente(1L);
            assertThrows(BusinessException.class, () -> service.actualizar(1L, otroRol));

            UsuarioEntity soloNombre = usuario("admin@prueba.co", Rol.ADMIN);
            soloNombre.setNombre("Otro nombre");
            assertEquals("Otro nombre", service.actualizar(1L, soloNombre).getNombre(), "sus datos sí los cambia");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void actualizar_ultimoAdministradorActivo_noSeQuita() {
        UsuarioEntity otro = usuario("admin2@prueba.co", Rol.ADMIN);
        otro.setId(2L);
        otro.setActivo(true);
        when(repository.findById(2L)).thenReturn(Optional.of(otro));
        UsuarioEntity inactivo = usuario("admin2@prueba.co", Rol.ADMIN);
        inactivo.setActivo(false);

        when(repository.countByRolAndActivo(Rol.ADMIN, true)).thenReturn(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.actualizar(2L, inactivo));
        assertEquals("Debe quedar al menos un administrador activo", error.getMessage());

        when(repository.countByRolAndActivo(Rol.ADMIN, true)).thenReturn(2L);
        assertFalse(service.actualizar(2L, inactivo).getActivo(), "con otro administrador activo, sí");
    }

    private static void iniciarSesion(Long id) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(id, "u" + id, "Prueba", Rol.ADMIN, null, null, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void asignarClientes_soloAPsicologosYConClientesExistentes() {
        UsuarioEntity cliente = usuario("rrhh@prueba.co", Rol.CLIENTE);
        when(repository.findById(7L)).thenReturn(Optional.of(cliente));
        assertThrows(BusinessException.class, () -> service.asignarClientes(7L, List.of(1L)));

        UsuarioEntity psicologo = usuario("psico@prueba.co", Rol.PSICOLOGO);
        when(repository.findById(5L)).thenReturn(Optional.of(psicologo));
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(clienteRepository.existsById(2L)).thenReturn(true);
        when(clienteRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.asignarClientes(5L, List.of(1L, 99L)));
        verify(usuarioClienteRepository, never()).borrarDeUsuario(any());

        assertEquals(List.of(1L, 2L), service.asignarClientes(5L, List.of(2L, 1L, 2L)), "sin repetidos y en orden");
        verify(usuarioClienteRepository).borrarDeUsuario(5L);
        verify(usuarioClienteRepository).saveAll(anyList());
    }

    @Test
    void cambiarContrasena_reemplazaElHash() {
        UsuarioEntity existente = usuario("admin@prueba.co", Rol.ADMIN);
        existente.setContrasenaHash(passwordEncoder.encode("anterior123"));
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        UsuarioEntity actualizado = service.cambiarContrasena(1L, "nueva12345");

        assertTrue(passwordEncoder.matches("nueva12345", actualizado.getContrasenaHash()));
        assertFalse(passwordEncoder.matches("anterior123", actualizado.getContrasenaHash()));
    }

}
