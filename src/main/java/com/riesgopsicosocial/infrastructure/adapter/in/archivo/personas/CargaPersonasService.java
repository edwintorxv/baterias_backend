package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.CatalogosCarga.Busqueda;
import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.ResultadoCarga.FilaCarga;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.VincularPersonaMapper;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.VincularPersonaRequest;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente.ClienteJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.ResultadoVinculacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.VinculacionPersonaService;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Carga de personas de un cliente desde la plantilla Excel. Cada fila pasa por las mismas reglas que el
 * formulario ({@link VincularPersonaRequest} + {@link VinculacionPersonaService}). Validar no escribe nada;
 * confirmar guarda todo en una transacción, o nada si alguna fila tiene errores.
 */
@Service
public class CargaPersonasService {

    public static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    static final String ERROR = "ERROR";
    private static final DateTimeFormatter DIA_MES_ANIO = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public record Plantilla(String nombreArchivo, byte[] contenido) {
    }

    /** Una fila ya revisada: la petición armada (si se pudo) y sus errores. */
    private record FilaRevisada(FilaArchivo fila, VincularPersonaRequest request, List<String> errores,
                                ResultadoVinculacion resultado) {
    }

    private final PlantillaPersonasExcel excel;
    private final CatalogosCargaLoader catalogosLoader;
    private final VinculacionPersonaService vinculacionService;
    private final ClienteJpaRepository clienteRepository;
    private final Validator validator;

    public CargaPersonasService(PlantillaPersonasExcel excel, CatalogosCargaLoader catalogosLoader,
                                VinculacionPersonaService vinculacionService, ClienteJpaRepository clienteRepository,
                                Validator validator) {
        this.excel = excel;
        this.catalogosLoader = catalogosLoader;
        this.vinculacionService = vinculacionService;
        this.clienteRepository = clienteRepository;
        this.validator = validator;
    }

    /** Siempre vacía: nunca trae personas ya cargadas (protección de datos). */
    @Transactional(readOnly = true)
    public Plantilla plantilla(Long idCliente) {
        ClienteEntity cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));
        return new Plantilla("plantilla_personas_" + cliente.getNit() + ".xlsx", excel.generar(catalogosLoader.cargar()));
    }

    @Transactional(readOnly = true)
    public ResultadoCarga validar(Long idCliente, byte[] archivo) {
        return resultado(revisar(idCliente, archivo), false);
    }

    /** Todo o nada: si alguna fila tiene errores, 409 con los errores y no se guarda ninguna. */
    @Transactional
    public ResultadoCarga confirmar(Long idCliente, byte[] archivo) {
        List<FilaRevisada> revisadas = revisar(idCliente, archivo);
        List<String> errores = revisadas.stream()
                .filter(r -> !r.errores().isEmpty())
                .map(r -> "Fila " + r.fila().numero() + ": " + String.join("; ", r.errores()))
                .toList();
        if (!errores.isEmpty()) {
            throw new BusinessException("El archivo tiene " + errores.size() + " fila(s) con errores; no se guardó ninguna",
                    errores);
        }

        List<FilaRevisada> guardadas = new ArrayList<>();
        for (FilaRevisada revisada : revisadas) {
            try {
                ResultadoVinculacion resultado = vinculacionService.vincular(idCliente,
                        VincularPersonaMapper.toPersona(revisada.request()),
                        VincularPersonaMapper.toRelacion(revisada.request())).resultado();
                guardadas.add(new FilaRevisada(revisada.fila(), revisada.request(), List.of(), resultado));
            } catch (BusinessException e) {
                throw new BusinessException("Fila " + revisada.fila().numero() + ": " + e.getMessage()
                        + ". No se guardó ninguna fila");
            }
        }
        return resultado(guardadas, true);
    }

    // ---------------------------------------------------------------------------------------------

    private List<FilaRevisada> revisar(Long idCliente, byte[] archivo) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
        }
        List<FilaArchivo> filas = excel.leer(archivo);
        if (filas.isEmpty()) {
            throw new BusinessException("El archivo no tiene personas: diligencie la hoja \""
                    + PlantillaPersonasExcel.HOJA_PERSONAS + "\" desde la fila 2");
        }
        CatalogosCarga catalogos = catalogosLoader.cargar();
        Map<String, List<Integer>> filasPorDocumento = filas.stream()
                .filter(f -> f.valor(ColumnaPersona.DOCUMENTO) != null)
                .collect(Collectors.groupingBy(f -> TextoNormalizado.de(f.valor(ColumnaPersona.DOCUMENTO)),
                        LinkedHashMap::new, Collectors.mapping(FilaArchivo::numero, Collectors.toList())));

        return filas.stream().map(fila -> revisar(idCliente, fila, catalogos, filasPorDocumento)).toList();
    }

    private FilaRevisada revisar(Long idCliente, FilaArchivo fila, CatalogosCarga catalogos,
                                 Map<String, List<Integer>> filasPorDocumento) {
        Lectura lectura = new Lectura(fila, catalogos);
        VincularPersonaRequest request = new VincularPersonaRequest(
                lectura.texto(ColumnaPersona.DOCUMENTO),
                lectura.texto(ColumnaPersona.NOMBRE),
                lectura.texto(ColumnaPersona.APELLIDO),
                lectura.lista(ColumnaPersona.SEXO),
                lectura.entero(ColumnaPersona.ANIO_NACIMIENTO),
                lectura.lista(ColumnaPersona.ESTADO_CIVIL),
                lectura.lista(ColumnaPersona.NIVEL_ACADEMICO),
                lectura.lista(ColumnaPersona.CIUDAD_RESIDENCIA),
                lectura.lista(ColumnaPersona.ESTRATO),
                lectura.lista(ColumnaPersona.TIPO_VIVIENDA),
                lectura.texto(ColumnaPersona.OCUPACION),
                lectura.entero(ColumnaPersona.PERSONAS_A_CARGO),
                lectura.lista(ColumnaPersona.CIUDAD_TRABAJO),
                lectura.texto(ColumnaPersona.ANTIGUEDAD),
                lectura.texto(ColumnaPersona.CARGO),
                lectura.lista(ColumnaPersona.TIPO_CARGO),
                lectura.lista(ColumnaPersona.TIEMPO_CARGO),
                lectura.texto(ColumnaPersona.AREA),
                lectura.lista(ColumnaPersona.TIPO_CONTRATO),
                lectura.lista(ColumnaPersona.HORAS),
                lectura.lista(ColumnaPersona.TIPO_SALARIO),
                lectura.fecha(ColumnaPersona.FECHA_VINCULACION));

        // Errores de validación en el orden de las columnas; si la celda ya tenía un error de lectura,
        // no se repite como "obligatorio".
        Map<ColumnaPersona, List<String>> errores = new EnumMap<>(lectura.errores);
        for (ConstraintViolation<VincularPersonaRequest> violacion : validator.validate(request)) {
            columnaDe(violacion.getPropertyPath().toString()).filter(c -> !lectura.errores.containsKey(c))
                    .ifPresent(c -> errores.computeIfAbsent(c, k -> new ArrayList<>()).add(violacion.getMessage()));
        }
        if (request.anioNacimiento() != null) {
            VinculacionPersonaService.errorAnioNacimiento(request.anioNacimiento())
                    .ifPresent(e -> errores.computeIfAbsent(ColumnaPersona.ANIO_NACIMIENTO, k -> new ArrayList<>()).add(e));
        }
        List<String> mensajes = new ArrayList<>(errores.values().stream().flatMap(List::stream).toList());

        String documento = request.numeroIdentificacion();
        if (documento != null) {
            List<Integer> repetidas = filasPorDocumento.getOrDefault(TextoNormalizado.de(documento), List.of());
            if (repetidas.size() > 1) {
                mensajes.add("El documento " + documento + " está repetido en las filas "
                        + repetidas.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            }
        }

        ResultadoVinculacion resultado = null;
        if (mensajes.isEmpty()) {
            resultado = vinculacionService.clasificar(idCliente, documento);
            if (resultado == ResultadoVinculacion.CAMBIA_EMPRESA) {
                vinculacionService.errorFechaCambio(idCliente, documento, request.fechaVinculacion()).ifPresent(mensajes::add);
            }
        }
        return new FilaRevisada(fila, request, List.copyOf(mensajes), mensajes.isEmpty() ? resultado : null);
    }

    private static Optional<ColumnaPersona> columnaDe(String propiedad) {
        return Arrays.stream(ColumnaPersona.values()).filter(c -> c.propiedad().equals(propiedad)).findFirst();
    }

    private static ResultadoCarga resultado(List<FilaRevisada> revisadas, boolean guardado) {
        Map<String, Integer> resumen = new LinkedHashMap<>();
        for (ResultadoVinculacion r : ResultadoVinculacion.values()) {
            resumen.put(r.name(), 0);
        }
        resumen.put(ERROR, 0);
        List<FilaCarga> filas = new ArrayList<>();
        for (FilaRevisada r : revisadas) {
            String resultado = r.errores().isEmpty() ? r.resultado().name() : ERROR;
            resumen.merge(resultado, 1, Integer::sum);
            String nombre = String.join(" ", Objects.requireNonNullElse(r.request().nombre(), ""),
                    Objects.requireNonNullElse(r.request().apellido(), "")).trim();
            filas.add(new FilaCarga(r.fila().numero(), r.request().numeroIdentificacion(), nombre, resultado, r.errores()));
        }
        return new ResultadoCarga(revisadas.size(), resumen, resumen.get(ERROR) == 0, guardado, filas);
    }

    /** Convierte las celdas de una fila y junta los errores de lectura por columna. */
    private static final class Lectura {

        private final FilaArchivo fila;
        private final CatalogosCarga catalogos;
        private final Map<ColumnaPersona, List<String>> errores = new EnumMap<>(ColumnaPersona.class);

        Lectura(FilaArchivo fila, CatalogosCarga catalogos) {
            this.fila = fila;
            this.catalogos = catalogos;
        }

        String texto(ColumnaPersona columna) {
            return fila.valor(columna);
        }

        Integer entero(ColumnaPersona columna) {
            String valor = fila.valor(columna);
            if (valor == null) {
                return null;
            }
            try {
                return Integer.valueOf(valor);
            } catch (NumberFormatException e) {
                return error(columna, "'" + valor + "' no es un número entero");
            }
        }

        Long lista(ColumnaPersona columna) {
            String valor = fila.valor(columna);
            if (valor == null) {
                return null;
            }
            Busqueda busqueda = catalogos.buscar(columna.lista(), valor);
            return busqueda.id() != null ? busqueda.id() : error(columna, busqueda.error());
        }

        /** yyyy-MM-dd (celda de fecha) o día/mes/año escrito como texto. */
        LocalDate fecha(ColumnaPersona columna) {
            String valor = fila.valor(columna);
            if (valor == null) {
                return null;
            }
            try {
                return valor.contains("/") ? LocalDate.parse(valor, DIA_MES_ANIO) : LocalDate.parse(valor);
            } catch (DateTimeParseException e) {
                return error(columna, "'" + valor + "' no es una fecha (día/mes/año)");
            }
        }

        private <T> T error(ColumnaPersona columna, String mensaje) {
            errores.computeIfAbsent(columna, k -> new ArrayList<>()).add(columna.titulo() + ": " + mensaje);
            return null;
        }

    }

}
