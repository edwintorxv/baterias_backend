package com.riesgopsicosocial.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Quién hace el cambio, para la auditoría manual de los servicios ({@code usuario_crea},
 * {@code usuario_modifica}). Sin sesión (p. ej. el primer administrador al arrancar), "sistema".
 */
public final class UsuarioActual {

    public static final String SISTEMA = "sistema";

    private UsuarioActual() {
    }

    public static String nombre() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                ? usuario.correo() : SISTEMA;
    }

    /** Id del usuario de la sesión; {@code null} sin sesión. */
    public static Long id() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                ? usuario.id() : null;
    }

}
