package com.riesgopsicosocial.shared.catalogo;

import com.riesgopsicosocial.shared.exception.BusinessException;

public class CatalogoNombreDuplicadoException extends BusinessException {

    public CatalogoNombreDuplicadoException(String entidad, String nombre) {
        super("Ya existe " + entidad + " con nombre: " + nombre);
    }
}
