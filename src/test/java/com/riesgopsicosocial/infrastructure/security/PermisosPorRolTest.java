package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * La tabla de permisos por rol, ruta por ruta. Un controlador de prueba responde 200 a todo: lo que
 * salga 401/403 lo decide solo la seguridad, sin servicios ni BD.
 */
@WebMvcTest(controllers = PermisosPorRolTest.Eco.class)
@Import({PermisosPorRolTest.Eco.class, SecurityConfig.class, JwtConfig.class, UsuarioJwtConverter.class,
        RespuestasSeguridad.class, AlcanceUsuario.class})
class PermisosPorRolTest {

    @RestController
    static class Eco {
        @RequestMapping("/**")
        String eco() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mvc;

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

    /** La psicóloga tiene asignado el cliente 1, y el usuario CLIENTE es de esa empresa: aquí solo cuenta el rol. */
    @BeforeEach
    void psicologaConElCliente1() {
        when(usuarioClienteRepository.findByFkUsuario(1L)).thenReturn(List.of(new UsuarioClienteEntity(1L, 1L)));
    }

    private record Caso(HttpMethod metodo, String ruta, Set<Rol> permitidos) {
    }

    private static Caso caso(HttpMethod metodo, String ruta, Rol... permitidos) {
        return new Caso(metodo, ruta, permitidos.length == 0 ? EnumSet.noneOf(Rol.class) : EnumSet.of(permitidos[0], permitidos));
    }

    private static final HttpMethod GET = HttpMethod.GET, POST = HttpMethod.POST, PUT = HttpMethod.PUT,
            DELETE = HttpMethod.DELETE;

    private static final List<Caso> TABLA = List.of(
            caso(GET, "/auth/yo", ADMIN, PSICOLOGO, CLIENTE, EVALUADO),
            caso(PUT, "/auth/contrasena", ADMIN, PSICOLOGO, CLIENTE, EVALUADO),

            caso(GET, "/usuarios", ADMIN),
            caso(POST, "/usuarios", ADMIN),
            caso(PUT, "/usuarios/1/clientes", ADMIN),

            caso(GET, "/preguntas", ADMIN, PSICOLOGO),
            caso(GET, "/cuestionarios", ADMIN, PSICOLOGO),
            caso(GET, "/opcion-respueta/1", ADMIN, PSICOLOGO),
            caso(POST, "/preguntas", ADMIN),
            caso(PUT, "/baremo-dominios/3", ADMIN),

            caso(GET, "/sexos", ADMIN, PSICOLOGO),
            caso(GET, "/ciudades-municipios/5", ADMIN, PSICOLOGO),
            caso(POST, "/sexos"),

            caso(GET, "/clientes", ADMIN, PSICOLOGO),
            caso(GET, "/clientes/1", ADMIN, PSICOLOGO),
            caso(POST, "/clientes", ADMIN),
            caso(PUT, "/clientes/1", ADMIN),
            caso(DELETE, "/clientes/1"),
            caso(GET, "/clientes/1/personas", PSICOLOGO),
            caso(POST, "/clientes/1/personas", PSICOLOGO),
            caso(GET, "/clientes/1/personas/plantilla", PSICOLOGO),
            caso(POST, "/clientes/1/personas/carga", PSICOLOGO),
            caso(PUT, "/clientes/1/personas"),

            caso(GET, "/clientes/1/evaluados/2/informe", PSICOLOGO),
            caso(GET, "/clientes/1/evaluados/2/informe/archivo", PSICOLOGO),

            caso(GET, "/clientes/1/informe", PSICOLOGO, CLIENTE),
            caso(GET, "/clientes/1/informe/anios", PSICOLOGO, CLIENTE),
            caso(GET, "/clientes/1/informe/archivo", PSICOLOGO, CLIENTE),
            caso(GET, "/clientes/1/informe/conclusiones", PSICOLOGO, CLIENTE),
            caso(PUT, "/clientes/1/informe/conclusiones", PSICOLOGO),

            caso(PUT, "/evaluadores/1/firma", ADMIN, PSICOLOGO),
            caso(GET, "/evaluadores/1/firma", ADMIN, PSICOLOGO),
            caso(GET, "/evaluadores", ADMIN, PSICOLOGO),
            caso(POST, "/evaluadores", ADMIN),
            caso(PUT, "/evaluadores/1", ADMIN),

            caso(POST, "/aplicaciones", PSICOLOGO),
            caso(POST, "/aplicaciones/1/resultados", PSICOLOGO),
            caso(GET, "/respuestas", PSICOLOGO),
            caso(PUT, "/evaluados/3", PSICOLOGO),
            caso(GET, "/evaluado-clientes", PSICOLOGO),

            caso(GET, "/ruta-que-nadie-clasifico"));

    private static UsernamePasswordAuthenticationToken sesion(Rol rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(1L, rol.name().toLowerCase(), "Prueba", rol,
                rol == PSICOLOGO ? 1L : null, rol == CLIENTE ? 1L : null, null);
        return new UsernamePasswordAuthenticationToken(usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }

    @Test
    void cadaRolSoloEntraDondeDiceLaTabla() throws Exception {
        List<String> errores = new ArrayList<>();
        for (Caso caso : TABLA) {
            for (Rol rol : Rol.values()) {
                int esperado = caso.permitidos().contains(rol) ? 200 : 403;
                int obtenido = mvc.perform(request(caso.metodo(), caso.ruta()).with(authentication(sesion(rol))))
                        .andReturn().getResponse().getStatus();
                if (obtenido != esperado) {
                    errores.add(caso.metodo() + " " + caso.ruta() + " como " + rol + ": " + obtenido + " (esperado " + esperado + ")");
                }
            }
        }
        assertTrue(errores.isEmpty(), String.join("\n", errores));
    }

    @Test
    void sinSesion_401EnTodoMenosElLogin() throws Exception {
        for (Caso caso : TABLA) {
            assertEquals(401, mvc.perform(request(caso.metodo(), caso.ruta())).andReturn().getResponse().getStatus(),
                    caso.metodo() + " " + caso.ruta());
        }
        assertEquals(200, mvc.perform(request(POST, "/auth/login")).andReturn().getResponse().getStatus());
    }

}
