package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Del token válido al usuario de la BD: si ya no existe, está inactivo o sus sesiones se cerraron después
 * de emitido el token (sesiones_desde, V23), 401 aunque el token no haya vencido. El rol sale de la BD (authority ROLE_ADMIN, ROLE_PSICOLOGO…), no del token.
 */
@Component
public class UsuarioJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioJpaRepository repository;

    public UsuarioJwtConverter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token sin usuario válido");
        }
        UsuarioAutenticado usuario = repository.findById(id)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> sesionVigente(u, jwt.getIssuedAt()))
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new InvalidBearerTokenException(
                        "El usuario del token no existe, está inactivo o su sesión fue cerrada"));
        return new UsernamePasswordAuthenticationToken(usuario, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
    }

    /** sesiones_desde está en la hora local (America/Bogota), como el resto de fechas de la BD. */
    private static boolean sesionVigente(UsuarioEntity usuario, Instant emitido) {
        if (usuario.getSesionesDesde() == null) {
            return true;
        }
        return emitido != null && !emitido.isBefore(usuario.getSesionesDesde().atZone(ZoneId.systemDefault()).toInstant());
    }

}
