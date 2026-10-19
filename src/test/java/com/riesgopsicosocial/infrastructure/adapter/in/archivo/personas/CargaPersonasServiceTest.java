package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.ResultadoCarga.FilaCarga;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.ResultadoVinculacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.Vinculacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.VinculacionPersonaService;
import com.riesgopsicosocial.shared.exception.BusinessException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CargaPersonasServiceTest {

    private final CatalogosCargaLoader catalogos = mock(CatalogosCargaLoader.class);
    private final VinculacionPersonaService vinculacion = mock(VinculacionPersonaService.class);
    private final ClienteJpaRepository clienteRepository = mock(ClienteJpaRepository.class);
    private final CargaPersonasService service = new CargaPersonasService(new PlantillaPersonasExcel(), catalogos,
            vinculacion, clienteRepository, Validation.buildDefaultValidatorFactory().getValidator());

    @BeforeEach
    void dependencias() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(catalogos.cargar()).thenReturn(ArchivoDePrueba.catalogos());
        when(vinculacion.clasificar(eq(1L), any())).thenReturn(ResultadoVinculacion.NUEVA);
        when(vinculacion.errorFechaCambio(any(), any(), any())).thenReturn(Optional.empty());
        when(vinculacion.vincular(eq(1L), any(), any()))
                .thenAnswer(i -> new Vinculacion(ResultadoVinculacion.NUEVA, i.getArgument(1), i.getArgument(2), false));
    }

    @Test
    void validar_filasCorrectas_diceQuePasaraSinGuardar() {
        when(vinculacion.clasificar(1L, "2020")).thenReturn(ResultadoVinculacion.ACTUALIZA);

        ResultadoCarga resultado = service.validar(1L, ArchivoDePrueba.archivo(
                ArchivoDePrueba.persona("1010"), ArchivoDePrueba.persona("2020")));

        assertTrue(resultado.puedeConfirmar());
        assertFalse(resultado.guardado());
        assertEquals(2, resultado.totalFilas());
        assertEquals(1, resultado.resumen().get("NUEVA"));
        assertEquals(1, resultado.resumen().get("ACTUALIZA"));
        assertEquals(0, resultado.resumen().get("ERROR"));
        assertEquals(new FilaCarga(2, "1010", "Ana Pérez", "NUEVA", List.of()), resultado.filas().get(0));
        verify(vinculacion, never()).vincular(any(), any(), any());
    }

    @Test
    void validar_erroresPorFilaEnElOrdenDeLasColumnas() {
        Map<ColumnaPersona, Object> conErrores = ArchivoDePrueba.persona("1010");
        conErrores.remove(ColumnaPersona.SEXO);
        conErrores.put(ColumnaPersona.NIVEL_ACADEMICO, "Bachiller");
        conErrores.put(ColumnaPersona.PERSONAS_A_CARGO, "dos");
        conErrores.put(ColumnaPersona.FECHA_VINCULACION, "31/02/2026");

        ResultadoCarga resultado = service.validar(1L, ArchivoDePrueba.archivo(conErrores));

        assertFalse(resultado.puedeConfirmar());
        assertEquals(List.of(
                "El sexo es obligatorio",
                "Nivel académico: 'Bachiller' no está en la lista",
                "Personas a cargo: 'dos' no es un número entero",
                "Fecha de vinculación: '31/02/2026' no es una fecha (día/mes/año)"), resultado.filas().get(0).errores());
        assertEquals("ERROR", resultado.filas().get(0).resultado());
        verify(vinculacion, never()).clasificar(any(), any());
    }

    @Test
    void validar_documentoRepetidoEnElArchivo_errorEnAmbasFilas() {
        ResultadoCarga resultado = service.validar(1L, ArchivoDePrueba.archivo(
                ArchivoDePrueba.persona("1010"), ArchivoDePrueba.persona("3030"), ArchivoDePrueba.persona("1010")));

        assertEquals(2, resultado.resumen().get("ERROR"));
        assertEquals(List.of("El documento 1010 está repetido en las filas 2, 4"), resultado.filas().get(2).errores());
        assertEquals("NUEVA", resultado.filas().get(1).resultado());
    }

    @Test
    void validar_anioFueraDeRangoYCambioDeEmpresaConFechaAnterior() {
        Map<ColumnaPersona, Object> menor = ArchivoDePrueba.persona("1010");
        menor.put(ColumnaPersona.ANIO_NACIMIENTO, (double) LocalDate.now().getYear() - 5);
        when(vinculacion.clasificar(1L, "2020")).thenReturn(ResultadoVinculacion.CAMBIA_EMPRESA);
        when(vinculacion.errorFechaCambio(1L, "2020", null)).thenReturn(Optional.of("fecha anterior"));

        ResultadoCarga resultado = service.validar(1L, ArchivoDePrueba.archivo(menor, ArchivoDePrueba.persona("2020")));

        assertTrue(resultado.filas().get(0).errores().get(0).startsWith("El año de nacimiento debe estar entre"));
        assertEquals(List.of("fecha anterior"), resultado.filas().get(1).errores());
    }

    @Test
    void confirmar_conErrores_409ConLasFilasYNoGuardaNada() {
        Map<ColumnaPersona, Object> sinNombre = ArchivoDePrueba.persona("2020");
        sinNombre.remove(ColumnaPersona.NOMBRE);

        BusinessException error = assertThrows(BusinessException.class, () -> service.confirmar(1L,
                ArchivoDePrueba.archivo(ArchivoDePrueba.persona("1010"), sinNombre)));

        assertEquals(List.of("Fila 3: El nombre es obligatorio"), error.getDetails());
        verify(vinculacion, never()).vincular(any(), any(), any());
    }

    @Test
    void confirmar_sinErrores_vinculaCadaFila() {
        ResultadoCarga resultado = service.confirmar(1L, ArchivoDePrueba.archivo(
                ArchivoDePrueba.persona("1010"), ArchivoDePrueba.persona("2020")));

        assertTrue(resultado.guardado());
        assertEquals(2, resultado.resumen().get("NUEVA"));
        verify(vinculacion, times(2)).vincular(eq(1L), any(), any());
    }

    @Test
    void archivoSinPersonas_409() {
        assertThrows(BusinessException.class, () -> service.validar(1L, ArchivoDePrueba.archivo()));
    }

}
