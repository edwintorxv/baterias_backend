package com.riesgopsicosocial.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS global para toda la API (ningún controlador lo declara). Orígenes desde
 * {@code app.cors.origenes} ({@code APP_CORS_ORIGENES}); sin valor, ninguno. Sin credenciales: el
 * token va en el encabezado Authorization, no en cookies. Se expone Content-Disposition para que el
 * front lea el nombre de los archivos descargados (informes en Word/PDF).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origenes:}") String origenes) {
        List<String> permitidos = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(permitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuracion.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

}
