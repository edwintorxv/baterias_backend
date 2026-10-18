package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Solo sus clientes": la psicóloga (id 2) tiene asignado el cliente 1; el usuario CLIENTE es de la
 * empresa 1. Rutas de prueba con las mismas variables que las reales.
 */
@WebMvcTest(controllers = AlcanceClienteTest.Eco.class)
@Import({AlcanceClienteTest.Eco.class, SecurityConfig.class, JwtConfig.class, UsuarioJwtConverter.class,
        RespuestasSeguridad.class, AlcanceUsuario.class})
class AlcanceClienteTest {

    @RestController
    static class Eco {
        @GetMapping("/clientes/{idCliente}/informe")
        String informe(@PathVariable Long idCliente) {
            return "ok";
        }

        @GetMapping("/clientes/{idCliente}/evaluados/{idEvaluado}/informe")
        String individual(@PathVariable Long idCliente, @PathVariable Long idEvaluado) {
            return "ok";
        }

        @GetMapping("/clientes/{id}")
        String cliente(@PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/evaluado-clientes/{id}")
        String relacion(@PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/aplicaciones/{id}")
        String aplicacion(@PathVariable Long id) {
            return "ok";
        }

        @PostMapping("/aplicaciones/{idAplicacion}/resultados")
        String calcular(@PathVariable Long idAplicacion) {
            return "ok";
        }

        @GetMapping("/respuestas/{id}")
        String respuesta(@PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/evaluados/{id}")
        String evaluado(@PathVariable Long id) {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AlcanceUsuario alcance;

    @MockitoBean
    private UsuarioJpaRepository repository;
    @MockitoBean
    private UsuarioClienteJpaRepository usuarioClienteRepository;
    @MockitoBean
    private EvaluadoClienteJpaRepository evaluadoClienteRepository;
    @MockitoBean
    private AplicacionJpaRepository aplicacionRepository;
    @MockitoBean
    private RespuestaJpaRepository respuestaRepository;

    /**
     * Cliente 1 (de la psicóloga) y cliente 2 (ajeno): relaciones 10 y 20, aplicaciones 100 y 200,
     * respuestas 1000 y 2000.
     */
    @BeforeEach
    void psicologaConElCliente1() {
        when(usuarioClienteRepository.findByFkUsuario(2L)).thenReturn(List.of(new UsuarioClienteEntity(2L, 1L)));
        for (long cliente : new long[]{1L, 2L}) {
            EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
            relacion.setId(cliente * 10);
            relacion.setFkCliente(cliente);
            when(evaluadoClienteRepository.findById(cliente * 10)).thenReturn(Optional.of(relacion));
            AplicacionEntity aplicacion = new AplicacionEntity();
            aplicacion.setId(cliente * 100);
            aplicacion.setFkEvaluadoCliente(cliente * 10);
            when(aplicacionRepository.findById(cliente * 100)).thenReturn(Optional.of(aplicacion));
            RespuestaEntity respuesta = new RespuestaEntity();
            respuesta.setId(cliente * 1000);
            respuesta.setFkAplicacion(cliente * 100);
            when(respuestaRepository.findById(cliente * 1000)).thenReturn(Optional.of(respuesta));
        }
        EvaluadoClienteEntity relacion1 = new EvaluadoClienteEntity();
        relacion1.setId(10L);
        relacion1.setFkEvaluado(5L);
        when(evaluadoClienteRepository.findByFkClienteIn(Set.of(1L))).thenReturn(List.of(relacion1));
        // Evaluado 5: cliente 1; 6: solo cliente 2; 7: sin ninguna relación; 8: clientes 1 y 2.
        when(evaluadoClienteRepository.findByFkEvaluado(5L)).thenReturn(List.of(relacionCon(1L)));
        when(evaluadoClienteRepository.findByFkEvaluado(6L)).thenReturn(List.of(relacionCon(2L)));
        when(evaluadoClienteRepository.findByFkEvaluado(7L)).thenReturn(List.of());
        when(evaluadoClienteRepository.findByFkEvaluado(8L)).thenReturn(List.of(relacionCon(2L), relacionCon(1L)));
    }

    private static EvaluadoClienteEntity relacionCon(Long cliente) {
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setFkCliente(cliente);
        return relacion;
    }

    @Test
    void psicologa_evaluados_deSusClientesOSinEmpresa() throws Exception {
        mvc.perform(get("/evaluados/5").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/evaluados/7").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/evaluados/8").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/evaluados/6").with(authentication(PSICOLOGA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evaluado no encontrado con id: 6"));

        SecurityContextHolder.getContext().setAuthentication(PSICOLOGA);
        assertEquals(Optional.of(Set.of(5L)), alcance.evaluadosVisibles());
    }

    @Test
    void firmante_elPsicologoSoloSeAsignaASiMismo() {
        SecurityContextHolder.getContext().setAuthentication(PSICOLOGA); // evaluador 7
        assertDoesNotThrow(() -> alcance.exigirFirmante(null, null), "sin evaluador");
        assertDoesNotThrow(() -> alcance.exigirFirmante(7L, 8L), "se asigna a sí mismo");
        assertDoesNotThrow(() -> alcance.exigirFirmante(8L, 8L), "conserva el que ya tenía");
        assertThrows(AccessDeniedException.class, () -> alcance.exigirFirmante(8L, null));
        assertThrows(AccessDeniedException.class, () -> alcance.exigirFirmante(8L, 9L));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken sesion(Rol rol, Long id, Long idEvaluador, Long idCliente) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(id, "u" + id, "Prueba", rol, idEvaluador, idCliente, null);
        return new UsernamePasswordAuthenticationToken(usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }

    private static final UsernamePasswordAuthenticationToken ADMIN = sesion(Rol.ADMIN, 1L, null, null);
    private static final UsernamePasswordAuthenticationToken PSICOLOGA = sesion(Rol.PSICOLOGO, 2L, 7L, null);
    private static final UsernamePasswordAuthenticationToken RRHH = sesion(Rol.CLIENTE, 3L, null, 1L);

    @Test
    void psicologa_soloSusClientesAsignados() throws Exception {
        mvc.perform(get("/clientes/1/informe").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/clientes/1/evaluados/5/informe").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/clientes/1").with(authentication(PSICOLOGA))).andExpect(status().isOk());

        mvc.perform(get("/clientes/2/informe").with(authentication(PSICOLOGA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado con id: 2"));
        mvc.perform(get("/clientes/2/evaluados/5/informe").with(authentication(PSICOLOGA))).andExpect(status().isNotFound());
        mvc.perform(get("/clientes/2").with(authentication(PSICOLOGA))).andExpect(status().isNotFound());
    }

    @Test
    void psicologa_relacionesAplicacionesResultadosYRespuestas_soloDeSusClientes() throws Exception {
        mvc.perform(get("/evaluado-clientes/10").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/aplicaciones/100").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(post("/aplicaciones/100/resultados").with(authentication(PSICOLOGA))).andExpect(status().isOk());
        mvc.perform(get("/respuestas/1000").with(authentication(PSICOLOGA))).andExpect(status().isOk());

        mvc.perform(get("/evaluado-clientes/20").with(authentication(PSICOLOGA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Relación evaluado-cliente no encontrada con id: 20"));
        mvc.perform(get("/aplicaciones/200").with(authentication(PSICOLOGA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aplicación no encontrada con id: 200"));
        mvc.perform(post("/aplicaciones/200/resultados").with(authentication(PSICOLOGA))).andExpect(status().isNotFound());
        mvc.perform(get("/respuestas/2000").with(authentication(PSICOLOGA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Respuesta no encontrada con id: 2000"));

        // Si no existe, pasa al controlador (que responde su propio 404).
        mvc.perform(get("/aplicaciones/999").with(authentication(PSICOLOGA))).andExpect(status().isOk());
    }

    @Test
    void relacionesVisibles_soloLasDeSusClientes() {
        SecurityContextHolder.getContext().setAuthentication(PSICOLOGA);
        assertEquals(Optional.of(Set.of(10L)), alcance.relacionesVisibles());
        SecurityContextHolder.getContext().setAuthentication(ADMIN);
        assertEquals(Optional.empty(), alcance.relacionesVisibles(), "todas");
        assertDoesNotThrow(() -> alcance.exigirAplicacion(200L), "el admin no tiene restricción de cliente");
    }

    @Test
    void cliente_soloSuEmpresa() throws Exception {
        mvc.perform(get("/clientes/1/informe").with(authentication(RRHH))).andExpect(status().isOk());
        mvc.perform(get("/clientes/2/informe").with(authentication(RRHH))).andExpect(status().isNotFound());
    }

    @Test
    void admin_veTodosLosClientes() throws Exception {
        mvc.perform(get("/clientes/2").with(authentication(ADMIN))).andExpect(status().isOk());
    }

    @Test
    void clientesVisibles_porRol() {
        SecurityContextHolder.getContext().setAuthentication(ADMIN);
        assertEquals(Optional.empty(), alcance.clientesVisibles(), "todos");
        SecurityContextHolder.getContext().setAuthentication(PSICOLOGA);
        assertEquals(Optional.of(Set.of(1L)), alcance.clientesVisibles());
        SecurityContextHolder.getContext().setAuthentication(RRHH);
        assertEquals(Optional.of(Set.of(1L)), alcance.clientesVisibles());
        SecurityContextHolder.getContext().setAuthentication(sesion(Rol.EVALUADO, 4L, null, null));
        assertEquals(Optional.of(Set.of()), alcance.clientesVisibles(), "ninguno");
        SecurityContextHolder.clearContext();
        assertEquals(Optional.of(Set.of()), alcance.clientesVisibles(), "sin sesión, ninguno");
    }

    @Test
    void firma_elPsicologoSoloLaSuya_elAdminCualquiera() {
        SecurityContextHolder.getContext().setAuthentication(PSICOLOGA);
        assertDoesNotThrow(() -> alcance.exigirEvaluadorPropio(7L));
        assertThrows(AccessDeniedException.class, () -> alcance.exigirEvaluadorPropio(8L));
        SecurityContextHolder.getContext().setAuthentication(ADMIN);
        assertDoesNotThrow(() -> alcance.exigirEvaluadorPropio(8L));
    }

}
