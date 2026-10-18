package com.riesgopsicosocial.infrastructure.adapter.in.rest.auth.dto;

import java.time.Instant;

/**
 * @param token     va en cada petición: {@code Authorization: Bearer <token>}
 * @param expiraEn  segundos de vigencia; al vencer hay que volver a iniciar sesión
 */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEn,
        Instant expira,
        UsuarioSesionResponse usuario
) {
}
