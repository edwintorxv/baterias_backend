package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado.BorradoResultados;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Aplicación 7 de prueba: forma A (grupo 1), atiende clientes y es jefe. */
class AplicacionServiceTest {

    private final AplicacionJpaRepository repository = mock(AplicacionJpaRepository.class);
    private final BorradoResultados borradoResultados = mock(BorradoResultados.class);
    private final AplicacionService service = new AplicacionService(repository, mock(EvaluadoClienteJpaRepository.class),
            mock(TipoCargoJpaRepository.class), mock(EvaluadorJpaRepository.class), borradoResultados);

    private final AplicacionEntity guardada = new AplicacionEntity();

    @BeforeEach
    void configuracion() {
        guardada.setId(7L);
        guardada.setFkEvaluadoCliente(3L);
        guardada.setFkGrupoOcupacional(1L);
        guardada.setAtiendeClientes(true);
        guardada.setEsJefe(true);
        when(repository.findById(7L)).thenReturn(Optional.of(guardada));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static AplicacionEntity datos(Long grupo, Boolean atiendeClientes, Boolean esJefe) {
        AplicacionEntity datos = new AplicacionEntity();
        datos.setFkEvaluadoCliente(3L);
        datos.setFkGrupoOcupacional(grupo);
        datos.setAtiendeClientes(atiendeClientes);
        datos.setEsJefe(esJefe);
        return datos;
    }

    @Test
    void actualizar_preguntaFiltroEnNull_quedaSinRegistrarYBorraLosResultados() {
        AplicacionEntity actualizada = service.actualizar(7L, datos(2L, true, null));

        assertEquals(2L, actualizada.getFkGrupoOcupacional());
        assertTrue(actualizada.getAtiendeClientes());
        assertNull(actualizada.getEsJefe(), "en la forma B no aplica: vuelve a no registrada");
        verify(borradoResultados).borrar(7L);
    }

    @Test
    void actualizar_sinCambiosEnLaBaseDelCalculo_noBorraLosResultados() {
        service.actualizar(7L, datos(1L, true, true));

        verify(borradoResultados, never()).borrar(any());
    }

}
