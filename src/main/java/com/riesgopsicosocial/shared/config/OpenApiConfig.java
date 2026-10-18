package com.riesgopsicosocial.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger con el botón "Authorize": se pega el token de POST /auth/login y Swagger lo envía en cada
 * petición (Authorization: Bearer …).
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "token";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API Batería de Riesgo Psicosocial")
                        .description("Inicie sesión en POST /auth/login y pegue el token en \"Authorize\"."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }

}
