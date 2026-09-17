package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio;

import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaremoDominioService {

    private final BaremoDominioJpaRepository repository;

    public BaremoDominioService(BaremoDominioJpaRepository repository) {
        this.repository = repository;
    }

    public BaremoDominioEntity crear(BaremoDominioEntity entidad) {
        return repository.save(entidad);
    }

    public BaremoDominioEntity actualizar(Long id, BaremoDominioEntity datos) {
        BaremoDominioEntity existente = obtenerPorId(id);

        existente.setFkDominioCuestionario(datos.getFkDominioCuestionario());
        existente.setFkNivelRiesgo(datos.getFkNivelRiesgo());
        existente.setValorMinimo(datos.getValorMinimo());
        existente.setValorMaximo(datos.getValorMaximo());

        return repository.save(existente);
    }

    public BaremoDominioEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Baremo de dominio no encontrado con id: " + id));
    }

    public List<BaremoDominioEntity> listarTodos() {
        return repository.findAll();
    }

    public List<BaremoDominioEntity> listarPorDominioCuestionario(Long fkDominioCuestionario) {
        return repository.findByFkDominioCuestionario(fkDominioCuestionario);
    }

    public List<BaremoDominioEntity> listarPorNivelRiesgo(Long fkNivelRiesgo) {
        return repository.findByFkNivelRiesgo(fkNivelRiesgo);
    }

}
