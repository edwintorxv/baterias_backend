package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.Rol;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;

/**
 * Quien hace la petición, leído de la BD en cada una (no del token): si se desactiva o cambia de
 * rol, aplica de inmediato aunque su token siga vigente.
 */
public record UsuarioAutenticado(Long id, String correo, String nombre, Rol rol,
                                 Long idEvaluador, Long idCliente, Long idEvaluado) {

    public static UsuarioAutenticado de(UsuarioEntity usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getNombre(), usuario.getRol(),
                usuario.getFkEvaluador(), usuario.getFkCliente(), usuario.getFkEvaluado());
    }

}
