package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteJpaRepository repository;

    public ClienteService(ClienteJpaRepository repository) {
        this.repository = repository;
    }

    public ClienteEntity crear(ClienteEntity entidad) {
        if (repository.existsByNit(entidad.getNit())) {
            throw new BusinessException("Ya existe un cliente con NIT: " + entidad.getNit());
        }

        entidad.setUsuarioCrea(UsuarioActual.nombre());
        return repository.save(entidad);

    }

    /** El NIT identifica al cliente y no se modifica: el request lo trae y debe ser el mismo (409 si no). */
    public ClienteEntity actualizar(Long id, ClienteEntity datos) {
        ClienteEntity existente = obtenerPorId(id);

        if (!existente.getNit().equalsIgnoreCase(datos.getNit().trim())) {
            throw new BusinessException("El NIT del cliente no se puede modificar (actual: " + existente.getNit() + ")");
        }

        existente.setFkIndustria(datos.getFkIndustria());
        existente.setFkCiudadMunicipio(datos.getFkCiudadMunicipio());
        existente.setNombre(datos.getNombre());
        existente.setDireccion(datos.getDireccion());
        existente.setTelefono(datos.getTelefono());
        existente.setNombreContacto(datos.getNombreContacto());
        existente.setCargoContacto(datos.getCargoContacto());
        existente.setTelefonoContacto(datos.getTelefonoContacto());

        existente.setFechaModificado(LocalDateTime.now());
        existente.setUsuarioModifica(UsuarioActual.nombre());
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
