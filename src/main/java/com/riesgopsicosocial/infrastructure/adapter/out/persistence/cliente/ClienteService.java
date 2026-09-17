package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteJpaRepository repository;

    public ClienteService(ClienteJpaRepository repository) {
        this.repository = repository;
    }

    public ClienteEntity crear(ClienteEntity entidad) {
        if (repository.existsByNit(entidad.getNit())) {
            throw new BusinessException("Ya existe un clinte con NIT: " + entidad.getNit());
        }

        return repository.save(entidad);

    }

    public ClienteEntity actualizar(Long id, ClienteEntity datos) {
        ClienteEntity existente = obtenerPorId(id);

        if (!existente.getNit().equalsIgnoreCase(datos.getNit())
                && repository.existsByNit(datos.getNit())) {
            throw new BusinessException("Ya existe un cliente con NIT: " + datos.getNit());
        }

        existente.setFkIndustria(datos.getFkIndustria());
        existente.setFkCiudadMunicipio(datos.getFkCiudadMunicipio());
        existente.setNit(datos.getNit());
        existente.setNombre(datos.getNombre());
        existente.setDireccion(datos.getDireccion());
        existente.setTelefono(datos.getTelefono());
        existente.setNombreContacto(datos.getNombreContacto());
        existente.setCargoContacto(datos.getCargoContacto());
        existente.setTelefonoContacto(datos.getTelefonoContacto());

        return repository.save(existente);
    }

    public ClienteEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    public List<ClienteEntity> listarTodos() {
        return repository.findAll();
    }


}
