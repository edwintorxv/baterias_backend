package com.riesgopsicosocial.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Todo recurso identificado en la URL solo se atiende si es de un cliente que quien pide ve; si no,
 * 404. Va antes del controlador, así ninguna ruta nueva bajo estos prefijos se olvida de revisarlo.
 * La plantilla de la ruta dice a qué apunta cada id:
 * <ul>
 *   <li>{@code /clientes/{idCliente}/…}, {@code /clientes/{id}}: cliente (informes y conclusiones)</li>
 *   <li>{@code /evaluado-clientes/{id}}: relación evaluado-cliente</li>
 *   <li>{@code /aplicaciones/{id}}, {@code /aplicaciones/{idAplicacion}/…}: aplicación (y sus resultados)</li>
 *   <li>{@code /respuestas/{id}}: respuesta</li>
 *   <li>{@code /evaluados/{id}}: evaluado (persona; ver {@link AlcanceUsuario#exigirEvaluado})</li>
 * </ul>
 * Lo que viene en el cuerpo (crear, editar) y los listados los revisa cada controlador.
 */
@Component
public class AlcanceClienteInterceptor implements HandlerInterceptor {

    static final String[] RUTAS = {"/clientes/**", "/evaluado-clientes/**", "/aplicaciones/**", "/respuestas/**",
            "/evaluados/**"};

    private final AlcanceUsuario alcance;

    public AlcanceClienteInterceptor(AlcanceUsuario alcance) {
        this.alcance = alcance;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String plantilla = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (variables == null || plantilla == null) {
            return true;
        }
        try {
            if (plantilla.startsWith("/clientes/")) {
                Long id = id(variables.containsKey("idCliente") ? variables.get("idCliente") : variables.get("id"));
                if (id != null) {
                    alcance.exigirCliente(id);
                }
            } else if (plantilla.startsWith("/evaluado-clientes/")) {
                alcance.exigirRelacion(id(variables.get("id")));
            } else if (plantilla.startsWith("/aplicaciones/")) {
                alcance.exigirAplicacion(id(variables.containsKey("idAplicacion") ? variables.get("idAplicacion") : variables.get("id")));
            } else if (plantilla.startsWith("/respuestas/")) {
                alcance.exigirRespuesta(id(variables.get("id")));
            } else if (plantilla.startsWith("/evaluados/")) {
                alcance.exigirEvaluado(id(variables.get("id")));
            }
        } catch (NumberFormatException e) {
            return true; // el controlador responde 400 por tipo inválido
        }
        return true;
    }

    private static Long id(String valor) {
        return valor == null ? null : Long.valueOf(valor);
    }

}
