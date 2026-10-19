package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** El NIT identifica al cliente: se fija al crear y no cambia al actualizar. */
class ClienteServiceTest {

    private final ClienteJpaRepository repository = mock(ClienteJpaRepository.class);
    private final ClienteService service = new ClienteService(repository);

    private static ClienteEntity cliente(String nit, String nombre) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setNit(nit);
        cliente.setNombre(nombre);
        return cliente;
    }

    @BeforeEach
    void guardarDevuelveLoMismo() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.findById(1L)).thenReturn(Optional.of(cliente("900123456", "Empresa")));
    }

    @Test
    void actualizar_conElMismoNit_actualizaLosDemasDatos() {
        ClienteEntity actualizado = service.actualizar(1L, cliente(" 900123456 ", "Empresa Nueva S.A.S."));

        assertEquals("900123456", actualizado.getNit());
        assertEquals("Empresa Nueva S.A.S.", actualizado.getNombre());
    }

    @Test
    void actualizar_conOtroNit_409SinGuardar() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.actualizar(1L, cliente("800999888", "Empresa")));

        assertEquals("El NIT del cliente no se puede modificar (actual: 900123456)", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void crear_conNitRepetido_409() {
        when(repository.existsByNit("900123456")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.crear(cliente("900123456", "Otra")));
        assertEquals("Ya existe un cliente con NIT: 900123456", ex.getMessage());
    }

}
