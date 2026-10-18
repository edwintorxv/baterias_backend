package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Del token válido al usuario de la BD: si ya no existe o está inactivo, 401 aunque el token no haya
 * vencido. El rol sale de la BD (authority ROLE_ADMIN, ROLE_PSICOLOGO…), no del token.
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
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new InvalidBearerTokenException("El usuario del token no existe o está inactivo"));
        return new UsernamePasswordAuthenticationToken(usuario, jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
    }

}
