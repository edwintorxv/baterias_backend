package com.riesgopsicosocial.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Tokens firmados con HS256 y un secreto propio (la API emite y valida sus tokens). Sin secreto,
 * o con uno corto, la app no arranca: nunca hay uno por defecto fuera del perfil dev.
 */
@Configuration
public class JwtConfig {

    /** HS256 pide una clave de al menos 256 bits. */
    private static final int MINIMO_BYTES = 32;

    private final SecretKey clave;

    public JwtConfig(@Value("${app.seguridad.jwt.secreto:}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMO_BYTES) {
            throw new IllegalStateException("Defina APP_SEGURIDAD_JWT_SECRETO con al menos " + MINIMO_BYTES
                    + " caracteres: firma los tokens de inicio de sesión");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

}
