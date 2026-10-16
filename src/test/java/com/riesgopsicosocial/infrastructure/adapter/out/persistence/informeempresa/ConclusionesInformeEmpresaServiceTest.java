package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador.EvaluadorJpaRepository;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConclusionesInformeEmpresaServiceTest {

    private final InformeEmpresaJpaRepository repository = mock(InformeEmpresaJpaRepository.class);
    private final ClienteJpaRepository clienteRepository = mock(ClienteJpaRepository.class);
    private final EvaluadoClienteJpaRepository evaluadoClienteRepository = mock(EvaluadoClienteJpaRepository.class);
    private final AplicacionJpaRepository aplicacionRepository = mock(AplicacionJpaRepository.class);
    private final EvaluadorJpaRepository evaluadorRepository = mock(EvaluadorJpaRepository.class);
    private final ConclusionesInformeEmpresaService service = new ConclusionesInformeEmpresaService(repository,
            clienteRepository, evaluadoClienteRepository, aplicacionRepository, evaluadorRepository);

    @BeforeEach
    void clienteConAplicacionesEn2026() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setId(10L);
        when(evaluadoClienteRepository.findByFkCliente(1L)).thenReturn(List.of(relacion));
        when(aplicacionRepository.buscarPorRelacionesYPeriodo(any(), any(), any())).thenReturn(List.of(new AplicacionEntity()));
        when(repository.findByFkClienteAndAnio(1L, 2026)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void consultar_sinNadaGuardado_devuelveVacioSinGuardar() {
        InformeEmpresaEntity informe = service.consultar(1L, 2026);

        assertNull(informe.getId());
        assertEquals(1L, informe.getFkCliente());
        assertEquals(2026, informe.getAnio());
        assertNull(informe.getConclusiones());
        verify(repository, never()).save(any());
    }

    @Test
    void guardar_creaLaFilaConTextosLimpiosYBlancosComoVacios() {
        InformeEmpresaEntity guardado = service.guardar(1L, 2026,
                datos("  Conclusiones del año.  ", "Corto", "   ", null, null));

        assertEquals("Conclusiones del año.", guardado.getConclusiones());
        assertEquals("Corto", guardado.getAccionesCortoPlazo());
        assertNull(guardado.getAccionesMedianoPlazo(), "en blanco = sin texto");
        assertNull(guardado.getAccionesLargoPlazo());
        assertNull(guardado.getFechaModificado(), "es nueva: la fecha de creación la pone la BD");
    }

    @Test
    void guardar_reemplazaTodoLoAnteriorYMarcaLaFechaDeModificacion() {
        InformeEmpresaEntity existente = datos("Viejas", "Viejo corto", "Viejo mediano", "Viejo largo", null);
        existente.setId(5L);
        existente.setFkCliente(1L);
        existente.setAnio(2026);
        when(repository.findByFkClienteAndAnio(1L, 2026)).thenReturn(Optional.of(existente));

        InformeEmpresaEntity guardado = service.guardar(1L, 2026, datos("Nuevas", null, null, null, null));

        assertEquals(5L, guardado.getId());
        assertEquals("Nuevas", guardado.getConclusiones());
        assertNull(guardado.getAccionesCortoPlazo(), "lo que no viene queda vacío");
        assertNotNull(guardado.getFechaModificado());
    }

    @Test
    void guardar_evaluadorInactivo_409() {
        when(evaluadorRepository.findById(7L)).thenReturn(Optional.of(evaluador(7L, false)));

        assertThrows(BusinessException.class, () -> service.guardar(1L, 2026, datos("x", null, null, null, 7L)));
        verify(repository, never()).save(any());
    }

    @Test
    void guardar_evaluadorQueYaTeniaAsignado_noSeRevalidaAunqueSeHayaRetirado() {
        InformeEmpresaEntity existente = datos("x", null, null, null, 7L);
        existente.setId(5L);
        when(repository.findByFkClienteAndAnio(1L, 2026)).thenReturn(Optional.of(existente));

        InformeEmpresaEntity guardado = service.guardar(1L, 2026, datos("y", null, null, null, 7L));

        assertEquals(7L, guardado.getFkEvaluador());
        verify(evaluadorRepository, never()).findById(any());
    }

    @Test
    void clienteInexistente_404() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.consultar(99L, 2026));
    }

    @Test
    void anioSinAplicaciones_404() {
        when(aplicacionRepository.buscarPorRelacionesYPeriodo(any(), any(), any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.guardar(1L, 2025, datos("x", null, null, null, null)));
    }

    private static InformeEmpresaEntity datos(String conclusiones, String corto, String mediano, String largo,
                                              Long fkEvaluador) {
        InformeEmpresaEntity datos = new InformeEmpresaEntity();
        datos.setConclusiones(conclusiones);
        datos.setAccionesCortoPlazo(corto);
        datos.setAccionesMedianoPlazo(mediano);
        datos.setAccionesLargoPlazo(largo);
        datos.setFkEvaluador(fkEvaluador);
        return datos;
    }

    private static EvaluadorEntity evaluador(Long id, boolean activo) {
        EvaluadorEntity evaluador = new EvaluadorEntity();
        evaluador.setId(id);
        evaluador.setActivo(activo);
        return evaluador;
    }

}
