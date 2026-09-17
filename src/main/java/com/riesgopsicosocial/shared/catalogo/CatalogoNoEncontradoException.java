package com.riesgopsicosocial.shared.catalogo;

import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;

public class CatalogoNoEncontradoException extends ResourceNotFoundException {

    public CatalogoNoEncontradoException(String entidad, Long id) {
        super("No se encontro " + entidad + " con id: " + id);
    }
}
