package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.CambioContrasenaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.DefinirContrasenaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.EnlaceContrasenaResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.LoginRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.LoginResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.UsuarioSesionResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EnlaceContrasenaService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.EstadoEnlace;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.security.AutenticacionService;
import com.riesgopsicosocial.infrastructure.security.UsuarioAutenticado;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Inicio de sesión y enlace de contraseña (abiertos), y datos/contraseña de quien tiene la sesión.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AutenticacionService service;
    private final EnlaceContrasenaService enlaceService;

    public AuthController(AutenticacionService service, EnlaceContrasenaService enlaceService) {
        this.service = service;
        this.enlaceService = enlaceService;
    }

    private static UsuarioSesionResponse toResponse(UsuarioAutenticado usuario) {
        return new UsuarioSesionResponse(usuario.id(), usuario.correo(), usuario.nombre(), usuario.rol(),
                usuario.idEvaluador(), usuario.idCliente());
    }

    private static LoginResponse toResponse(AutenticacionService.Sesion sesion) {
        return new LoginResponse(sesion.token().valor(), "Bearer", sesion.token().segundos(), sesion.token().expira(),
                toResponse(sesion.usuario()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest http) {
        AutenticacionService.Sesion sesion = service.iniciarSesion(request.correo(), request.contrasena(),
                http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok(ResponseBuilder.success("Sesión iniciada", toResponse(sesion)));
    }

    @GetMapping("/yo")
    public ResponseEntity<ApiResponse<UsuarioSesionResponse>> yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(ResponseBuilder.success("Usuario de la sesión", toResponse(usuario)));
    }

    /**
     * Cambia la propia contraseña y cierra las sesiones de los demás equipos. Devuelve un token nuevo (como el
     * login) para que esta sesión siga abierta: el anterior ya no sirve. 400 si la actual no coincide o la
     * nueva es igual; 429 tras varios intentos fallidos (mismo freno del login).
     */
    @PutMapping("/contrasena")
    public ResponseEntity<ApiResponse<LoginResponse>> cambiarContrasena(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                                        @Valid @RequestBody CambioContrasenaRequest request,
                                                                        HttpServletRequest http) {
        AutenticacionService.Sesion sesion = service.cambiarContrasena(usuario, request.actual(), request.nueva(),
                http.getRemoteAddr());
        return ResponseEntity.ok(ResponseBuilder.success(
                "Contraseña actualizada. Se cerraron sus sesiones en otros equipos", toResponse(sesion)));
    }

    /** Público: si el enlace sirve y para quién (siempre 200; el estado dice si se puede usar). */
    @GetMapping("/enlace")
    public ResponseEntity<ApiResponse<EnlaceContrasenaResponse>> consultarEnlace(@RequestParam String codigo) {
        EnlaceContrasenaService.ConsultaEnlace consulta = enlaceService.consultar(codigo);
        boolean vigente = consulta.estado() == EstadoEnlace.VIGENTE;
        return ResponseEntity.ok(ResponseBuilder.success(vigente ? "Enlace vigente" : consulta.estado().mensaje(),
                new EnlaceContrasenaResponse(vigente, consulta.estado(), consulta.estado().mensaje(),
                        consulta.proposito(), consulta.nombre(), consulta.correo(), consulta.vence())));
    }

    /**
     * Público: define la contraseña con el enlace (un solo uso) y cierra las sesiones abiertas. Devuelve
     * el correo para llevarlo al inicio de sesión; no inicia sesión. 409 si el enlace no está vigente.
     */
    @PostMapping("/enlace")
    public ResponseEntity<ApiResponse<String>> definirContrasena(@Valid @RequestBody DefinirContrasenaRequest request) {
        UsuarioEntity usuario = enlaceService.definirContrasena(request.codigo(), request.contrasena());
        return ResponseEntity.ok(ResponseBuilder.success("Contraseña definida: ya puede iniciar sesión", usuario.getCorreo()));
    }

}
