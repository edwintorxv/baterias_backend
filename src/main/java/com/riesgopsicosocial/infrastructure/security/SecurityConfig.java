package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin sesión: cada petición trae su token (Authorization: Bearer …). Abiertos solo el inicio de
 * sesión, Swagger y la página de error. Permisos por rol según la tabla acordada con el usuario
 * (CLAUDE.md); lo que no esté clasificado aquí queda bloqueado para todos, para que un endpoint nuevo
 * no quede abierto por olvido. El orden importa: gana la primera regla que coincide.
 * Sin CSRF: no hay cookies de sesión que un sitio ajeno pueda aprovechar.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = Rol.ADMIN.name();
    private static final String PSICOLOGO = Rol.PSICOLOGO.name();
    private static final String CLIENTE = Rol.CLIENTE.name();

    /** Estructura de la batería: la mantiene el administrador; el psicólogo la consulta. */
    static final String[] CONFIGURACION = {"/cuestionarios/**", "/preguntas/**", "/dominios/**", "/dimensiones/**",
            "/dominio-cuestionarios/**", "/dimension-cuestionarios/**", "/baremo-dominios/**",
            "/baremo-dimensiones/**", "/escala/**", "/opcion-respueta/**", "/niveles-riesgos/**",
            "/grupo-ocupacional/**"};

    /** Catálogos de solo lectura para los formularios. */
    static final String[] CATALOGOS = {"/departamentos/**", "/ciudades-municipios/**", "/sexos/**",
            "/estados-civiles/**", "/niveles-academicos/**", "/estratos-socioeconomicos/**", "/tipos-viviendas/**",
            "/tipo-cargo/**", "/tipo-contrato/**", "/tipo-salario/**", "/tiempo-cargo/**", "/horas-labor/**",
            "/industrias/**", "/sectores-economicos/**"};

    /** Trabajo del psicólogo con los trabajadores (datos confidenciales). */
    static final String[] TRABAJO_PSICOLOGO = {"/evaluados/**", "/evaluado-clientes/**", "/aplicaciones/**",
            "/respuestas/**"};

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, UsuarioJwtConverter converter,
                                               RespuestasSeguridad respuestas) throws Exception {
        http
                .cors(Customizer.withDefaults()) // CorsConfig: va antes de pedir token, para el preflight
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/auth/**").authenticated()

                        .requestMatchers("/usuarios/**").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.GET, CONFIGURACION).hasAnyRole(ADMIN, PSICOLOGO)
                        .requestMatchers(CONFIGURACION).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, CATALOGOS).hasAnyRole(ADMIN, PSICOLOGO)

                        // Informes: el individual es historia clínica (solo el psicólogo); el de empresa
                        // también lo ve el cliente, pero las conclusiones las escribe el psicólogo.
                        .requestMatchers("/clientes/*/evaluados/**").hasRole(PSICOLOGO)
                        .requestMatchers(HttpMethod.GET, "/clientes/*/personas").hasRole(PSICOLOGO)
                        .requestMatchers(HttpMethod.POST, "/clientes/*/personas").hasRole(PSICOLOGO)
                        .requestMatchers(HttpMethod.GET, "/clientes/*/informe/**").hasAnyRole(PSICOLOGO, CLIENTE)
                        .requestMatchers(HttpMethod.PUT, "/clientes/*/informe/conclusiones").hasRole(PSICOLOGO)
                        .requestMatchers(HttpMethod.GET, "/clientes", "/clientes/*").hasAnyRole(ADMIN, PSICOLOGO)
                        .requestMatchers(HttpMethod.POST, "/clientes").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/clientes/*").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.PUT, "/evaluadores/*/firma").hasAnyRole(ADMIN, PSICOLOGO)
                        .requestMatchers(HttpMethod.GET, "/evaluadores/**").hasAnyRole(ADMIN, PSICOLOGO)
                        .requestMatchers(HttpMethod.POST, "/evaluadores").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/evaluadores/*").hasRole(ADMIN)

                        .requestMatchers(TRABAJO_PSICOLOGO).hasRole(PSICOLOGO)

                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(respuestas.noAutenticado())
                        .accessDeniedHandler(respuestas.sinPermiso()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas.noAutenticado())
                        .accessDeniedHandler(respuestas.sinPermiso()));
        return http.build();
    }

}
