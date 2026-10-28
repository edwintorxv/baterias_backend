package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.CambioContrasenaRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.LoginRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.LoginResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto.UsuarioSesionResponse;
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

/** Inicio de sesión (abierto) y datos/contraseña de quien tiene la sesión. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AutenticacionService service;

    public AuthController(AutenticacionService service) {
        this.service = service;
    }

    private static UsuarioSesionResponse toResponse(UsuarioAutenticado usuario) {
        return new UsuarioSesionResponse(usuario.id(), usuario.correo(), usuario.nombre(), usuario.rol(),
                usuario.idEvaluador(), usuario.idCliente());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest http) {
        AutenticacionService.Sesion sesion = service.iniciarSesion(request.correo(), request.contrasena(),
                http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok(ResponseBuilder.success("Sesión iniciada", new LoginResponse(
                sesion.token().valor(), "Bearer", sesion.token().segundos(), sesion.token().expira(),
                toResponse(sesion.usuario()))));
    }

    @GetMapping("/yo")
    public ResponseEntity<ApiResponse<UsuarioSesionResponse>> yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(ResponseBuilder.success("Usuario de la sesión", toResponse(usuario)));
    }

    @PutMapping("/contrasena")
    public ResponseEntity<ApiResponse<Void>> cambiarContrasena(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                               @Valid @RequestBody CambioContrasenaRequest request) {
        service.cambiarContrasena(usuario, request.actual(), request.nueva());
        return ResponseEntity.ok(ResponseBuilder.success("Contraseña actualizada exitosamente", null));
    }

}
