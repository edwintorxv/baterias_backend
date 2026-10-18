package com.riesgopsicosocial.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riesgopsicosocial.shared.exception.ErrorCode;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 y 403 con el mismo formato ApiError que el resto de la API. */
@Component
public class RespuestasSeguridad {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Sin token, vencido, mal firmado o de un usuario inactivo. */
    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Debe iniciar sesión: el token no se envió, venció o no es válido", ErrorCode.UNAUTHORIZED);
        };
    }

    /** Con sesión, pero su rol no tiene permiso. */
    public AccessDeniedHandler sinPermiso() {
        return (request, response, ex) -> escribir(response, HttpServletResponse.SC_FORBIDDEN,
                "No tiene permiso para esta operación", ErrorCode.FORBIDDEN);
    }

    private void escribir(HttpServletResponse response, int estado, String mensaje, ErrorCode codigo) throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ResponseBuilder.error(mensaje, codigo.name(), null));
    }

}
