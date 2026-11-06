package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import com.riesgopsicosocial.infrastructure.security.UsuarioAutenticado;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EnlaceContrasenaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 6, 10, 30, 15, 700_000_000);

    private final UsuarioJpaRepository usuarioRepository = mock(UsuarioJpaRepository.class);
    private final EnlaceContrasenaJpaRepository enlaceRepository = mock(EnlaceContrasenaJpaRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EnlaceContrasenaService service = new EnlaceContrasenaService(usuarioRepository, enlaceRepository,
            eventos, Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));

    private UsuarioEntity usuario;

    @BeforeEach
    void preparar() {
        usuario = new UsuarioEntity();
        usuario.setId(5L);
        usuario.setCorreo("ana@prueba.co");
        usuario.setNombre("Ana");
        usuario.setRol(Rol.PSICOLOGO);
        usuario.setActivo(true);
        usuario.setContrasenaHash("$2a$hash-anterior");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        // Sesión del administrador (id 1) que genera el enlace.
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin@prueba.co", "Admin", Rol.ADMIN, null, null, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generar_quitaLaContrasena_cierraSesiones_anulaLosAnteriores_yEnviaElCodigo() {
        service.generar(5L);

        assertNull(usuario.getContrasenaHash(), "la contraseña anterior deja de servir");
        assertEquals(AHORA.withNano(0), usuario.getSesionesDesde(), "truncado a segundos, como el iat");
        assertEquals("admin@prueba.co", usuario.getUsuarioModifica());
        verify(usuarioRepository).save(usuario);
        verify(enlaceRepository).anularSinUsar(5L, AHORA, "admin@prueba.co");

        ArgumentCaptor<EnlaceContrasenaEntity> enlace = ArgumentCaptor.forClass(EnlaceContrasenaEntity.class);
        verify(enlaceRepository).save(enlace.capture());
        ArgumentCaptor<EnlaceContrasenaGenerado> evento = ArgumentCaptor.forClass(EnlaceContrasenaGenerado.class);
        verify(eventos).publishEvent(evento.capture());

        assertEquals(PropositoEnlace.RESTABLECER, enlace.getValue().getProposito());
        assertEquals(AHORA.plusHours(48), enlace.getValue().getVence());
        assertEquals("admin@prueba.co", enlace.getValue().getUsuarioCrea());
        String codigo = evento.getValue().codigo();
        assertTrue(codigo.length() >= 43, "32 bytes en Base64 URL");
        assertNotEquals(codigo, enlace.getValue().getCodigoHash(), "en la BD solo el hash");
        assertEquals(EnlaceContrasenaService.hash(codigo), enlace.getValue().getCodigoHash());
        assertEquals("ana@prueba.co", evento.getValue().correo());
        assertFalse(evento.getValue().toString().contains(codigo), "el código no sale en los logs");
    }

    @Test
    void generar_cadaVezUnCodigoDistinto() {
        service.generar(5L);
        service.generar(5L);

        ArgumentCaptor<EnlaceContrasenaGenerado> eventosCapturados = ArgumentCaptor.forClass(EnlaceContrasenaGenerado.class);
        verify(eventos, times(2)).publishEvent(eventosCapturados.capture());
        assertNotEquals(eventosCapturados.getAllValues().get(0).codigo(), eventosCapturados.getAllValues().get(1).codigo());
    }

    @Test
    void generar_sinContrasenaNiEnlacesAnteriores_esInvitacion() {
        usuario.setContrasenaHash(null);
        when(enlaceRepository.yaTuvoContrasena(5L)).thenReturn(false);

        service.generar(5L);

        ArgumentCaptor<EnlaceContrasenaEntity> enlace = ArgumentCaptor.forClass(EnlaceContrasenaEntity.class);
        verify(enlaceRepository).save(enlace.capture());
        assertEquals(PropositoEnlace.INVITACION, enlace.getValue().getProposito());
    }

    @Test
    void generar_sinContrasenaPeroYaLaTuvo_esRestablecer() {
        usuario.setContrasenaHash(null);
        when(enlaceRepository.yaTuvoContrasena(5L)).thenReturn(true);

        service.generar(5L);

        ArgumentCaptor<EnlaceContrasenaEntity> enlace = ArgumentCaptor.forClass(EnlaceContrasenaEntity.class);
        verify(enlaceRepository).save(enlace.capture());
        assertEquals(PropositoEnlace.RESTABLECER, enlace.getValue().getProposito());
    }

    @Test
    void generar_usuarioInactivo_409SinTocarNada() {
        usuario.setActivo(false);

        assertThrows(BusinessException.class, () -> service.generar(5L));
        assertNotNull(usuario.getContrasenaHash());
        verifyNoInteractions(eventos);
        verify(enlaceRepository, never()).anularSinUsar(any(), any(), anyString());
    }

    @Test
    void generar_aSiMismo_409() {
        usuario.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(BusinessException.class, () -> service.generar(1L));
        assertNotNull(usuario.getContrasenaHash());
        verifyNoInteractions(eventos);
    }

    @Test
    void generar_usuarioInexistente_404() {
        assertThrows(ResourceNotFoundException.class, () -> service.generar(99L));
        verify(enlaceRepository, never()).save(any());
        verifyNoInteractions(eventos);
    }

}
