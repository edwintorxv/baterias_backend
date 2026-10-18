package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS global con los orígenes del perfil dev (cualquier puerto de localhost). */
@WebMvcTest(controllers = CorsTest.Eco.class)
@Import({CorsTest.Eco.class, SecurityConfig.class, CorsConfig.class, JwtConfig.class, UsuarioJwtConverter.class,
        RespuestasSeguridad.class, AlcanceUsuario.class})
class CorsTest {

    @RestController
    static class Eco {
        @GetMapping("/clientes/1/informe")
        String informe() {
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

    @Test
    void preflightDelFront_permitidoSinToken() throws Exception {
        mvc.perform(options("/clientes/1/informe")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS"));
    }

    @Test
    void respuestaAlFront_exponeElNombreDelArchivo() throws Exception {
        mvc.perform(get("/clientes/1/informe").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized()) // sin token, pero con los encabezados CORS
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
    }

    @Test
    void origenAjeno_rechazado() throws Exception {
        mvc.perform(options("/clientes/1/informe")
                        .header("Origin", "https://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

}
