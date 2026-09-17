package com.riesgopsicosocial.shared.catalogo;

import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.data.crossstore.ChangeSetPersister;

public class CatalogoPadreNoEncontradoException extends ValidationException {

    public CatalogoPadreNoEncontradoException(String nombrePadre, Long idPadre) {
        super("El " + nombrePadre + " con id " + idPadre + " no existe");
    }

}
