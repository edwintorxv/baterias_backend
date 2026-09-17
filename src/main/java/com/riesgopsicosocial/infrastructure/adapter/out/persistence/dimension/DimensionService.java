package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio.DominioJpaRepository;
import com.riesgopsicosocial.shared.catalogo.CatalogoConPadreCrudService;
import com.riesgopsicosocial.shared.catalogo.CatalogoNombreDuplicadoException;
import org.springframework.stereotype.Service;

@Service
public class DimensionService extends CatalogoConPadreCrudService<DimensionEntity> {

    private final DimensionJpaRespository dimensionJpaRespository;

    public DimensionService(DimensionJpaRespository respository, DominioJpaRepository dominioJpaRepository) {
        super(respository, "Dimension", "Dominio", dominioJpaRepository::existsById);
        this.dimensionJpaRespository = respository;
    }

    @Override
    public DimensionEntity crear(DimensionEntity entidad) {
        entidad.setNombre(entidad.getNombre().trim());
        validarPadreExiste(entidad);
        if (dimensionJpaRespository.existsByFkDominioAndNombreIgnoreCase(entidad.getFkDominio(), entidad.getNombre())) {
            throw new CatalogoNombreDuplicadoException("Dimension", entidad.getNombre());
        }
        return dimensionJpaRespository.save(entidad);
    }

    @Override
    public DimensionEntity actualizar(Long id, DimensionEntity entidad) {
        DimensionEntity existente = obtenerPorId(id);
        entidad.setNombre(entidad.getNombre().trim());
        validarPadreExiste(entidad);

        boolean cambioDatos = !existente.getFkDominio().equals(entidad.getFkDominio())
                || !existente.getNombre().equalsIgnoreCase(entidad.getNombre());

        if (cambioDatos && dimensionJpaRespository.existsByFkDominioAndNombreIgnoreCase(entidad.getFkDominio(), entidad.getNombre())) {
            throw new CatalogoNombreDuplicadoException("Dimension", entidad.getNombre());
        }

        existente.setFkDominio(entidad.getFkDominio());
        existente.setNombre(entidad.getNombre());
        return dimensionJpaRespository.save(existente);
    }


}
