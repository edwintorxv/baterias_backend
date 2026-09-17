package com.riesgopsicosocial.shared.catalogo;

import java.util.List;
import java.util.function.Function;

public abstract class CatalogoConPadreCrudService<T extends Identificable & Nombrable & ConPadre> extends CatalogoCrudService<T> {

    private final String nombrePadre;
    private final Function<Long, Boolean> existePadre;

    protected CatalogoConPadreCrudService(CatalogoJpaRepository<T> repository,
                                  String nombreEntidad,
                                  String nombrePadre,
                                  Function<Long, Boolean> existePadre) {
        super(repository, nombreEntidad);
        this.nombrePadre = nombrePadre;
        this.existePadre = existePadre;
    }

    @Override
    public T crear(T entidad) {
        validarPadreExiste(entidad);
        return super.crear(entidad);
    }

    @Override
    public T actualizar(Long id, T entidad) {
        validarPadreExiste(entidad);
        return super.actualizar(id, entidad);
    }

    protected void validarPadreExiste(T entidad) {
        if (entidad.getIdPadre() == null || !existePadre.apply(entidad.getIdPadre())) {
            throw new CatalogoPadreNoEncontradoException(nombrePadre, entidad.getIdPadre());
        }
    }

}
