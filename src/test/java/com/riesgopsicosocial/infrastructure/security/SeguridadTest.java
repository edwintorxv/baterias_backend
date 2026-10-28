package com.riesgopsicosocial.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.AuthController;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.ResultadoIngreso;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioService;
import com.riesgopsicosocial.shared.config.ContrasenaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Seguridad real (filtros, JWT, respuestas 401) con el repositorio simulado: no toca la BD. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, UsuarioJwtConverter.class,
        RespuestasSeguridad.class, AutenticacionService.class, ContrasenaConfig.class, AlcanceUsuario.class,
        LimiteIntentosLogin.class})
class SeguridadTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioJpaRepository repository;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private UsuarioClienteJpaRepository usuarioClienteRepository;
    @MockitoBean
    private EvaluadoClienteJpaRepository evaluadoClienteRepository;
    @MockitoBean
    private AplicacionJpaRepository aplicacionRepository;
    @MockitoBean
    private RespuestaJpaRepository respuestaRepository;
    @MockitoBean
    private RegistroIngresos registroIngresos;

    private UsuarioEntity psicologa;

    @BeforeEach
    void usuarioPsicologa() {
        psicologa = new UsuarioEntity();
        psicologa.setId(2L);
        psicologa.setCorreo("psicologa@prueba.co");
        psicologa.setNombre("Psicóloga de Prueba");
        psicologa.setRol(Rol.PSICOLOGO);
        psicologa.setFkEvaluador(1L);
        psicologa.setActivo(true);
        psicologa.setContrasenaHash(passwordEncoder.encode("secreta123"));
        when(repository.findByCorreo("psicologa@prueba.co")).thenReturn(Optional.of(psicologa));
        when(repository.findById(2L)).thenReturn(Optional.of(psicologa));
    }

    private String login(String correo, String contrasena) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void sinToken_401ConFormatoApiError() throws Exception {
        mvc.perform(get("/auth/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void login_devuelveTokenQueSirveParaLasPeticiones() throws Exception {
        JsonNode respuesta = objectMapper.readTree(login(" Psicologa@Prueba.co ", "secreta123"));
        assertEquals("Bearer", respuesta.at("/data/tipo").asText());
        assertEquals(8 * 3600, respuesta.at("/data/expiraEn").asLong());
        assertEquals("PSICOLOGO", respuesta.at("/data/usuario/rol").asText());
        String token = respuesta.at("/data/token").asText();
        verify(registroIngresos).registrar(eq("psicologa@prueba.co"), eq(2L), eq("127.0.0.1"), isNull(),
                eq(ResultadoIngreso.EXITOSO));

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value("psicologa@prueba.co"))
                .andExpect(jsonPath("$.data.idEvaluador").value(1));
    }

    @Test
    void login_fallido_mismoMensajeSinDecirQueFallo() throws Exception {
        String malaContrasena = login("psicologa@prueba.co", "otra-clave");
        String noExiste = login("nadie@prueba.co", "secreta123");
        psicologa.setActivo(false);
        String inactivo = login("psicologa@prueba.co", "secreta123");

        for (String respuesta : new String[]{malaContrasena, noExiste, inactivo}) {
            JsonNode json = objectMapper.readTree(respuesta);
            assertEquals("UNAUTHORIZED", json.at("/errorCode").asText(), respuesta);
            assertEquals(AutenticacionService.CREDENCIALES_INVALIDAS, json.at("/message").asText());
        }
        verify(registroIngresos, times(2)).registrar(eq("psicologa@prueba.co"), eq(2L), any(), any(),
                eq(ResultadoIngreso.FALLIDO));
        verify(registroIngresos).registrar(eq("nadie@prueba.co"), isNull(), any(), any(),
                eq(ResultadoIngreso.FALLIDO));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"psicologa@prueba.co\",\"contrasena\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** Usuario e IP propios: el limitador es el mismo bean en todos los tests de la clase. */
    @Test
    void login_bloqueadoTrasCincoFallos_429AunqueLaContrasenaSeaCorrecta() throws Exception {
        UsuarioEntity rrhh = new UsuarioEntity();
        rrhh.setId(3L);
        rrhh.setCorreo("rrhh@prueba.co");
        rrhh.setNombre("Recursos Humanos");
        rrhh.setRol(Rol.CLIENTE);
        rrhh.setActivo(true);
        rrhh.setContrasenaHash(passwordEncoder.encode("secreta123"));
        when(repository.findByCorreo("rrhh@prueba.co")).thenReturn(Optional.of(rrhh));

        for (int i = 0; i < 5; i++) {
            loginDesde("rrhh@prueba.co", "mala", "10.9.9.9").andExpect(status().isUnauthorized());
        }
        clearInvocations(repository);

        loginDesde("rrhh@prueba.co", "secreta123", "10.9.9.9")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Demasiados intentos fallidos. Intente de nuevo en 15 minutos"));
        verify(repository, never()).findByCorreo(anyString());

        loginDesde("rrhh@prueba.co", "secreta123", "10.9.9.9").andExpect(status().isTooManyRequests());
        verify(registroIngresos, times(5)).registrar(eq("rrhh@prueba.co"), eq(3L), eq("10.9.9.9"), any(),
                eq(ResultadoIngreso.FALLIDO));
        verify(registroIngresos, times(1)).registrar(eq("rrhh@prueba.co"), isNull(), eq("10.9.9.9"), any(),
                eq(ResultadoIngreso.BLOQUEADO));
    }

    private ResultActions loginDesde(String correo, String contrasena,
                                      String ip) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}")
                .header("User-Agent", "Prueba/1.0")
                .with(peticion -> {
                    peticion.setRemoteAddr(ip);
                    return peticion;
                }));
    }

    @Test
    void tokenMalFirmado_401() throws Exception {
        String token = tokenService.emitir(psicologa).valor();
        String alterado = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencido_401() throws Exception {
        Clock hace9Horas = Clock.fixed(Instant.now().minus(Duration.ofHours(9)), ZoneOffset.UTC);
        String vencido = new TokenService(jwtEncoder, Duration.ofHours(8), hace9Horas).emitir(psicologa).valor();

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesactivadoConTokenVigente_401() throws Exception {
        String token = tokenService.emitir(psicologa).valor();
        psicologa.setActivo(false);

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

}
