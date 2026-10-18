package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite el token de inicio de sesión. Lleva el id del usuario en {@code sub}; el rol y el nombre
 * van solo como información para el front: los permisos se toman de la BD en cada petición.
 */
@Service
public class TokenService {

    static final String EMISOR = "riesgo-psicosocial";

    private final JwtEncoder encoder;
    private final Duration duracion;
    private final Clock reloj;

    @Autowired
    public TokenService(JwtEncoder encoder, @Value("${app.seguridad.jwt.duracion:8h}") Duration duracion) {
        this(encoder, duracion, Clock.systemUTC());
    }

    TokenService(JwtEncoder encoder, Duration duracion, Clock reloj) {
        this.encoder = encoder;
        this.duracion = duracion;
        this.reloj = reloj;
    }

    public Token emitir(UsuarioEntity usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("usuario", usuario.getUsuario())
                .claim("nombre", usuario.getNombre())
                .claim("rol", usuario.getRol().name())
                .build();
        String valor = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(valor, expira, duracion.toSeconds());
    }

    /** @param segundos vigencia del token desde que se emite */
    public record Token(String valor, Instant expira, long segundos) {
    }

}
