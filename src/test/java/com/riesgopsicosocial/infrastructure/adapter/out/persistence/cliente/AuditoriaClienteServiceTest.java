package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Auditoría manual: quién crea y quién modifica, tomado de la sesión. */
class AuditoriaClienteServiceTest {

    private final ClienteJpaRepository repository = mock(ClienteJpaRepository.class);
    private final ClienteService service = new ClienteService(repository);

    @BeforeEach
    void sesionDelAdmin() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", "Administrador", Rol.ADMIN, null, null, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static ClienteEntity cliente(String nit) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setNit(nit);
        cliente.setNombre("Empresa");
        return cliente;
    }

    @Test
    void crear_guardaQuienLoCreo() {
        ClienteEntity creado = service.crear(cliente("900"));

        assertEquals("admin", creado.getUsuarioCrea());
        assertNull(creado.getUsuarioModifica());
        assertNull(creado.getFechaModificado());
    }

    @Test
    void actualizar_guardaQuienYCuandoLoModifico_sinTocarQuienLoCreo() {
        ClienteEntity existente = cliente("900");
        existente.setUsuarioCrea("otro");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        ClienteEntity actualizado = service.actualizar(1L, cliente("900"));

        assertEquals("otro", actualizado.getUsuarioCrea());
        assertEquals("admin", actualizado.getUsuarioModifica());
        assertNotNull(actualizado.getFechaModificado());
    }

    @Test
    void sinSesion_quedaComoSistema() {
        SecurityContextHolder.clearContext();

        assertEquals(UsuarioActual.SISTEMA, service.crear(cliente("901")).getUsuarioCrea());
    }

}
