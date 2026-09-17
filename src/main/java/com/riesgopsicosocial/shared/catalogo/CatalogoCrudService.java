package com.riesgopsicosocial.shared.catalogo;

import java.util.List;

public abstract class CatalogoCrudService<T extends Identificable & Nombrable> {

    private final CatalogoJpaRepository<T> repository;
    private final String nombreEntidad;

    protected CatalogoCrudService(CatalogoJpaRepository<T> repository, String nombreEntidad) {
        this.repository = repository;
        this.nombreEntidad = nombreEntidad;
    }

    public T crear(T entidad) {
        String nombreNormalizado = normalizar(entidad.getNombre());
        if (repository.existsByNombre(nombreNormalizado)) {
            throw new CatalogoNombreDuplicadoException(nombreEntidad, nombreNormalizado);
        }
        return repository.save(entidad);
    }

    public T actualizar(Long id, T entidad) {
        T existente = obtenerPorId(id);
        String nombreNormalizado = normalizar(entidad.getNombre());
        boolean cambioNombre = !existente.getNombre().equalsIgnoreCase(nombreNormalizado);
        if (cambioNombre && repository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new CatalogoNombreDuplicadoException(nombreEntidad, nombreNormalizado);
        }

        existente.setNombre(nombreNormalizado);
        return repository.save(existente);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    public T obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new CatalogoNoEncontradoException(nombreEntidad, id)
                );
    }

    public List<T> listarTodos() {
        return repository.findAll();
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new CatalogoNoEncontradoException(nombreEntidad, id);
        }

        repository.deleteById(id);
    }


}
