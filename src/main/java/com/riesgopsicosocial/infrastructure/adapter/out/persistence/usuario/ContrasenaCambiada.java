package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import java.time.LocalDateTime;

/**
 * Se publica al definir o cambiar la contraseña; el aviso por correo sale después del commit. Nunca lleva la
 * contraseña.
 *
 * @param primeraVez la definió por la invitación (aún no tenía): cambia el texto del aviso
 */
public record ContrasenaCambiada(String correo, String nombre, LocalDateTime fecha, boolean primeraVez) {
}
