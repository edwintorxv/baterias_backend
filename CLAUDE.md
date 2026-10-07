# Contexto del Proyecto — Sistema de Baterías de Riesgo Psicosocial

> Documento de contexto para continuar el desarrollo desde IntelliJ (Claude Pro / Claude Code).
> Generado a partir de una sesión de diseño e implementación del backend.
> Última actualización: 2026-10-04 (motor de cálculo implementado; ver sección 2).

---

## 0. Puesta en marcha en otro computador

Requisitos: **JDK 17**, **PostgreSQL 16** (la máquina original usa `C:\Program Files\PostgreSQL\16`), Maven vía `./mvnw` (incluido en el repo). Repo: `https://github.com/edwintorxv/baterias_backend.git`, rama `main`.

1. Crear la BD vacía: `CREATE DATABASE bateria_psicosocial;` (usuario `postgres` / clave `1234`, según `application-dev.properties`; si cambian, ajustar ese archivo localmente sin subirlo).
2. **Ojo — los datos maestros NO están en las migraciones.** `V1__initial_schema.sql` solo crea tablas (los `INSERT` se quitaron, ver incidente 6.1) y `V2` solo inserta `grupo_ocupacional`. Catálogos, cuestionarios, dimensiones/dominios, preguntas, escalas, baremos y los datos de prueba (aplicaciones 1 y 2) viven únicamente en la BD local de la máquina original. Para tenerlos en la nueva hay dos caminos:
   - **Copiar la BD completa (recomendado)**: en la máquina original
     `& "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" -U postgres -d bateria_psicosocial -F c -f bateria_psicosocial.dump`
     y en la nueva (con la BD vacía creada)
     `pg_restore -U postgres -d bateria_psicosocial bateria_psicosocial.dump`.
     Esto trae también `flyway_schema_history`, así que Flyway no vuelve a aplicar V1/V2. El `.dump` contiene datos de evaluados: **no subirlo a git**; pasarlo por otro medio.
   - **BD vacía**: dejar que Flyway aplique V1 y V2 al arrancar (funcionan sobre tablas vacías) y cargar los datos maestros a mano. Pendiente a futuro: crear una migración `V6__datos_maestros.sql` (o la siguiente libre) con esos `INSERT` (ya con los valores corregidos por V3/V4); como V3–V5 no hacen nada sobre una BD vacía, esa migración debe traer los datos ya corregidos y terminar con `setval` de las secuencias para que el repo sea autosuficiente.
3. Arrancar: `./mvnw spring-boot:run` (perfil `dev` por defecto; en macOS/Linux, si `mvnw` no tiene permiso de ejecución porque viene de Windows, usar `sh mvnw spring-boot:run`). API en `http://localhost:8080/api`, Swagger en `http://localhost:8080/api/swagger-ui.html`.
4. Tests: `./mvnw test` (incluye `CalculadoraResultadoTest`, lógica pura sin BD).
5. La carpeta `logs/` está en `.gitignore` (se generan localmente al arrancar).

---

## 1. Stack y arquitectura

- **Spring Boot 3.5.16**, **Java 17**, **PostgreSQL**, **Flyway**, **MapStruct**, **Lombok**
- Arquitectura **hexagonal parcial**: no todas las ~34 tablas la usan completa.

### Estructura de paquetes

```
domain/       → solo módulos con lógica de negocio real (reservado para motor de cálculo de riesgo)
application/  → solo módulos con lógica de negocio real (idem)
infrastructure/adapter/in/rest/{entidad}/           → Controller
infrastructure/adapter/in/rest/{entidad}/dto/       → Request/Response (records)
infrastructure/adapter/out/persistence/{entidad}/   → Entity + JpaRepository + Service
shared/catalogo/  → CRUD genérico reutilizable para catálogos simples
shared/exception/ → GlobalExceptionHandler + BusinessException/ResourceNotFoundException/ValidationException
shared/response/  → ApiResponse, ApiError, ResponseBuilder
```

### Decisión clave de arquitectura

No todas las tablas usan hexagonal completo (`domain` + `application`). Los catálogos simples usan un patrón CRUD directo (`Controller → Service → JpaRepository`) apoyado en `shared/catalogo`. El hexagonal completo se reserva para el **Grupo 5 / motor de cálculo de riesgo** (`resultado_dimension`, `resultado_dominio`, `resultado_cuestionario`), y posiblemente partes del Grupo D.

---

## 2. Clasificación de tablas por verbos HTTP

- **Grupo A** (solo `GET` listar, sin escritura): 16 catálogos — ✅ completado
- **Grupo B** (`GET` + `GET/{id}`, filtro opcional por FK vía `@RequestParam`): `industria`, `ciudad_municipio` — ✅ completado
- **Grupo C** (`POST`, `PUT`, `GET`, `GET/{id}`, sin `DELETE`):
  - Ya implementados antes de esta sesión: `cuestionario`, `dominio`, `dimension`, `opcion_respuesta` (se les quitó el `DELETE`) — ✅
  - Implementados en esta sesión: `cliente`, `evaluado`, `evaluado_cliente`, `aplicacion` — ✅
  - Implementados en sesión posterior: `dimension_cuestionario`, `dominio_cuestionario`, `pregunta`, `baremo_dimension`, `baremo_dominio` — ✅ (Grupo C completo)
- **Grupo D** (transaccional, sin listar todo genérico, sin `delete`):
  - `respuesta` (`POST`, `GET/{id}`, `GET` filtrado por `@RequestParam` **obligatorio** `fkAplicacion`, **sin `PUT`** — inmutable una vez creada) — ✅ implementado. Decisiones tomadas:
    - Consulta: `GET /respuestas?fkAplicacion=X` (mismo patrón `@RequestParam` que el resto del proyecto, `fkAplicacion` obligatorio en vez de opcional porque no hay listado general).
    - `valor_obtenido`: lo calcula el backend. `RespuestaService.crear` busca la `PreguntaEntity` (para obtener `fkEscala`) y luego en `EscalaDetalleJpaRepository.findByFkEscalaAndFkOpcionRespuesta` el valor correspondiente; si no existe esa combinación lanza `BusinessException` (409). Decisión del usuario: las tablas `escala`, `opcion_respuesta` y `escala_detalle` se diseñaron justamente para que el backend calcule ese valor, no para que lo mande el frontend.
    - `PUT`: no existe. Una respuesta ya registrada es inmutable; si hay un error se maneja a nivel de aplicación completa, no editando la respuesta puntual.
    - Se creó `EscalaDetalleEntity` + `EscalaDetalleJpaRepository` (paquete `escaladetalle`) como pieza de soporte interna — no tenía controller/CRUD propio definido en ninguna sesión anterior y solo se usa como lookup desde `RespuestaService`. Si en el futuro hace falta gestionar sus valores por API, agregar el molde catálogo/entidad plana que corresponda.
  - `resultado_cuestionario`, `resultado_dimension`, `resultado_dominio` (motor de cálculo de riesgo) — **pendiente**, decisión de alcance tomada: se implementan en una etapa aparte, con diseño hexagonal completo (`domain`/`application`), ya que agregan puntajes de `respuesta`, aplican `factor_transformacion` de `dimension_cuestionario`/`dominio_cuestionario` y determinan `nivel_riesgo` según los rangos de `baremo_dimension`/`baremo_dominio`/`baremo_cuestionario`. Falta definir el disparador (¿se calcula automáticamente al finalizar la `aplicacion`, o mediante un endpoint explícito?).

- **Migración `V2__grupo_ocupacional_y_metodo_calculo.sql`** (preparación del motor de cálculo):
  - Catálogo `grupo_ocupacional` (1 = Jefes, profesionales y técnicos; 2 = Auxiliares y operarios). `tipo_cargo.fk_grupo_ocupacional` (cargos 1,2 → grupo 1; 3,4 → grupo 2).
  - `aplicacion.fk_grupo_ocupacional` NOT NULL: **foto del grupo al momento de aplicar** (decisión del usuario, "opción 2"). `AplicacionService.crear` lo deriva de `evaluado_cliente → tipo_cargo` si el request no lo manda; así un cambio de cargo posterior no altera los baremos de resultados históricos.
  - `baremo_dimension`/`baremo_dominio`/`baremo_cuestionario.fk_grupo_ocupacional` NOT NULL: los cuestionarios C (extralaboral) y D (estrés) tienen baremos distintos por grupo. Los baremos existentes de C se asignaron al grupo 1 — **verificar contra el manual** y cargar el juego del grupo 2.
  - `cuestionario.factor_transformacion` (A=492, B=388, C=124, D=61.16) y `cuestionario.metodo_calculo` (enum `domain.model.resultado.configuracion.MetodoCalculo`: `SUMA_POR_DOMINIOS` para A/B, `SUMA_DIRECTA` para C, `PROMEDIO_PONDERADO` para D).
  - `dimension_cuestionario.peso` (solo D: fisiológicos 4, comportamiento social 3, intelectuales 2, psicoemocionales 1). Total estrés = Σ promedio(dimensión) × peso, transformado = bruto / 61.16 × 100. El manual no trae baremos por dimensión para D, solo total.
  - `UNIQUE (fk_aplicacion, ...)` en `resultado_dominio` y `resultado_cuestionario`.
  - Aclaración: los cuestionarios C y D son **los mismos para todos los cargos** (mismas preguntas y respuestas); lo único que varía por grupo es el baremo con el que se interpreta el puntaje. Por eso el grupo vive en `baremo_*` y en `aplicacion`, nunca en `respuesta`.
  - Estado de datos de prueba (2026-09-24): las aplicaciones 1 y 2 (evaluados con cargo "Profesional" → grupo 1) tienen 123 respuestas cada una, **solo forma A**. Para probar el flujo completo con jefes faltan: las respuestas de C (31) y D (31) en esas aplicaciones, y 10 filas en `baremo_cuestionario` (total C y total D, grupo 1). Los baremos del grupo 2 para C/D se cargan después, antes de evaluar auxiliares/operarios.
  - La V2 ya está aplicada en la BD local (confirmado en `flyway_schema_history` el 2026-09-25).
- **Comportamiento esperado del motor si falta un baremo**: `BusinessException` (409) indicando qué falta (cuestionario/dimensión, grupo, puntaje); nunca guardar resultados parciales en silencio.
- **Disparador del cálculo**: endpoint explícito `POST /aplicaciones/{id}/resultados` (recalculable: borra y reinserta en la misma transacción) + `GET /aplicaciones/{id}/resultados`.

**Motor de cálculo — ✅ implementado (2026-09-25)**:
  - Estructura (organizada por módulo `resultado` en cada capa):
    - `domain/model/resultado/` → resultados (`ResultadoAplicacion`, `ResultadoCuestionario`, `ResultadoDominio`, `ResultadoDimension`, `NivelRiesgo`); `domain/model/resultado/configuracion/` → entrada del cálculo (`Configuracion*`, `RangoBaremo`, `RespuestaCalculo`, `MetodoCalculo`).
    - `domain/service/resultado/CalculadoraResultado` (lógica pura, sin Spring; test en `src/test/.../domain/service/resultado/CalculadoraResultadoTest`).
    - `application/port/in/resultado/` (`CalcularResultadosUseCase`, `ConsultarResultadosUseCase`), `application/port/out/resultado/` (`DatosCalculoPort`, `ResultadoPort`), `application/service/resultado/ResultadoAplicacionService`.
    - Persistencia: una carpeta por tabla (`resultadodimension/`, `resultadodominio/`, `resultadocuestionario/`, `baremocuestionario/`: Entity + JpaRepository) y `persistence/resultado/` con los adaptadores (`DatosCalculoPersistenceAdapter`, `ResultadoPersistenceAdapter`) + `CatalogoCalculoLoader` (configuración y niveles, compartido; los adaptadores no dependen entre sí).
    - REST: `infrastructure/adapter/in/rest/resultado/ResultadoController` + `dto/`.
  - Reglas: transformado = bruto / factor × 100 redondeado a 1 decimal (HALF_UP); solo se calculan los cuestionarios con al menos una respuesta; dentro de uno, todas las dimensiones deben estar completas (si no → 409); D solo guarda total (sin baremo por dimensión); C guarda dimensiones + total; A/B dimensiones + dominios + total.
  - Probado: aplicaciones 1 y 2 (forma A) → 46.3 y 47.6, "Riesgo muy alto" (recalculado tras la V3; los valores anteriores 57.9/52.4 estaban mal por la escala invertida).
  - **Migración `V3__corregir_escalas_y_dimensiones_forma_c.sql`** (2026-10-07): (1) los valores de `escala_detalle` de las escalas 1 y 2 estaban invertidos; según la Tabla 21 del manual, escala 1 = Siempre 0 … Nunca 4 (ítems positivos) y escala 2 = Siempre 4 … Nunca 0. La asignación de ítems a cada escala se verificó contra el manual y coincide 100 %: forma A (Tabla 21, 73 + 50) y forma B (Tabla 22, 68 + 29) y forma C (Tabla 11, 23 + 8). Forma D (escalas 3/4/5 = 9-6-3-0, 6-4-2-0, 3-2-1-0; sin "Casi nunca") también verificada: 9 + 13 + 9 ítems correctos. (2) Se recalculó `respuesta.valor_obtenido` (había además una respuesta de A con valor 9.00). (3) Forma C: ítems 18, 19, 20, 21, 23 → "Comunicación y relaciones interpersonales" (dimensión 22); "Relaciones familiares" (dimensión 21) queda con 22, 25, 27.
  - **Migración `V4__baremos_extralaboral_y_estres.sql`** (2026-10-07): carga los totales de C (Tablas 17/18 del manual extralaboral) y D (Tabla 6 del manual de estrés) para los dos grupos en `baremo_cuestionario`, y las dimensiones de C del grupo 2 (Tabla 18). Reemplazó 5 filas de C grupo 1 que se habían cargado a mano con la Tabla 34 (esa tabla es el **total general** intra + extra, no el total de C). Los niveles de estrés (muy bajo … muy alto) se mapean por posición a `nivel_riesgo` 1..5. Verificado también: dimensiones de C grupo 1 = Tabla 17 y totales de A/B = Tabla 33.
  - **Migración `V5__sincronizar_secuencias.sql`**: los datos se cargaron con ids explícitos y varias secuencias quedaron atrasadas (`aplicacion`, `evaluado_cliente`, `pregunta`, `baremo_dominio`, `sector_economico`), lo que hacía fallar los `POST` con llave duplicada. La V5 adelanta todas las secuencias a `MAX(id)`. **Regla**: si se cargan datos con ids explícitos, terminar con `setval`.
  - **Total general (pendiente de implementar)**: puntaje bruto = bruto intralaboral (A o B) + bruto extralaboral (C); transformado = bruto / factor × 100, factor 616 con A y 512 con B (Tabla 15 del manual extralaboral = suma de los factores de cada cuestionario). Baremos (Tabla 34): A+C 0–18,8 / 18,9–24,4 / 24,5–29,5 / 29,6–35,4 / 35,5–100; B+C 0–19,9 / 20,0–24,8 / 24,9–29,5 / 29,6–35,4 / 35,5–100.
  - Manuales en PDF (fuera del repo): `/Users/edwingacha/proyectos/Documentacion/baterias/` (en macOS se extrae el texto con `osascript -l JavaScript` + PDFKit, porque no hay poppler).
  - **Pendiente**: ítems condicionales de forma A/B (atención a clientes, jefes con colaboradores) — hoy se exige respuesta a todo; revisar en el manual cómo se califican cuando no aplican. Puntaje total general (intralaboral + extralaboral) no implementado.

---

## 3. Patrón de código — dos moldes distintos dentro de Grupo C

### 3.1 Molde "catálogo simple" (con `Identificable`/`Nombrable`)

Usado en: catálogos de Grupo A/B y en `cuestionario`, `dominio`, `dimension`, `opcion_respuesta` (Grupo C).

```java
@Service
public class CuestionarioService extends CatalogoCrudService<CuestionarioEntity> {
    public CuestionarioService(CuestionarioJpaRepository repository) {
        super(repository, "Cuestionario");
    }
}

public interface CuestionarioJpaRepository extends CatalogoJpaRepository<CuestionarioEntity> { }

@NoRepositoryBean
public interface CatalogoJpaRepository<T> extends JpaRepository<T, Long> {
    boolean existsByNombre(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
```

`CatalogoCrudService<T extends Identificable & Nombrable>` (clase base completa):

```java
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
                .orElseThrow(() -> new CatalogoNoEncontradoException(nombreEntidad, id));
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
```

**Regla de decisión**: este molde solo aplica cuando la entidad se identifica de forma única por un campo `nombre` real (no cuando `nombre` es solo descriptivo).

### 3.2 Molde "entidad plana" (service propio, sin `CatalogoCrudService`)

Usado en: `cliente`, `evaluado`, `evaluado_cliente`, `aplicacion` — y se usará en el resto de Grupo C pendiente.

Razón: ninguna de estas tablas tiene una clave de negocio de tipo `nombre` único (`cliente` usa `nit`, `evaluado` usa `numero_identificacion`, `evaluado_cliente`/`aplicacion` no tienen ni campo `nombre`). Forzar `Identificable`/`Nombrable` aquí generaría semántica falsa (ver discusión completa en la sección 5).

Estructura por tabla (4 archivos + DTOs):
- `{Entidad}Entity` (JPA)
- `{Entidad}JpaRepository` (`extends JpaRepository<Entidad, Long>` directo, **no** `CatalogoJpaRepository`)
- `{Entidad}Service` (clase propia, sin herencia)
- `{Entidad}Controller`
- `{Entidad}Request` / `{Entidad}Response` (records, en subpaquete `dto/`)

Cada `Service` implementa su propia validación de unicidad/integridad de negocio (no genérica):
- `cliente` → único por `nit`
- `evaluado` → único por `numero_identificacion`
- `evaluado_cliente` → único **activo** por (`fk_evaluado`, `fk_cliente`) — no se puede repetir una relación activa; si se reactiva tras estar inactiva, se permite crear una fila nueva (no se fuerza reactivación de la anterior)
- `aplicacion` → sin unicidad; `fecha_aplicacion` y `estado` se completan manualmente en el service si el request no los manda (`LocalDateTime.now()` y `"FINALIZADA"` respectivamente), porque Hibernate ignora los `DEFAULT` de Postgres al mandar `INSERT` con esos campos en `null`

---

## 4. Piezas transversales (`shared/`)

```java
public final class ResponseBuilder {
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static ApiError error(String message, String errorCode, List<String> details) {
        return ApiError.builder()
                .success(false).message(message).errorCode(errorCode)
                .details(details).timestamp(LocalDateTime.now()).build();
    }
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}

@Data @Builder
public class ApiError {
    private boolean success;
    private String message;
    private String errorCode;
    private List<String> details;
    private LocalDateTime timestamp;
}
```

Excepciones:
```java
public class BusinessException extends RuntimeException { /* ... */ }
public class ResourceNotFoundException extends RuntimeException { /* ... */ }
public class ValidationException extends RuntimeException { /* con List<String> details */ }

// Específicas de shared/catalogo, extienden las genéricas:
public class CatalogoNombreDuplicadoException extends BusinessException { /* ... */ }
public class CatalogoNoEncontradoException extends ResourceNotFoundException { /* ... */ }
```

`GlobalExceptionHandler` (`@RestControllerAdvice`) maneja, en este orden de especificidad:
1. `ResourceNotFoundException` → 404
2. `BusinessException` → 409
3. `ValidationException` → 400 (con `details`)
4. `MethodArgumentNotValidException` (bean validation `@Valid`) → 400
5. **`DataIntegrityViolationException`** (agregado en esta sesión) → 409, cubre violaciones de FK/UNIQUE que se escapan a nivel de Postgres (ej. `fk_industria` inexistente). Requiere agregar `DATA_INTEGRITY_VIOLATION` al enum `ErrorCode` (no confirmado si ya se agregó — **verificar**).
6. `Exception.class` genérico → 500 (con log de error)

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
    ApiError error = ResponseBuilder.error(
            "La operación viola una restricción de integridad de datos (verifique que las referencias existan y no haya duplicados)",
            ErrorCode.DATA_INTEGRITY_VIOLATION.name(), null);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
}
```

**Auditoría**: los campos `fecha_creado`, `fecha_modificado`, `usuario_crea`, `usuario_modifica` **no se gestionan automáticamente** (no hay `@EnableJpaAuditing` ni `AuditorAware`). `fecha_creado` se mapea con `insertable=false, updatable=false` (la BD la pone por `DEFAULT`); el resto quedan en `NULL` salvo que se seteen manualmente en el futuro. Decisión explícita del usuario: no se implementa auditoría automática porque "tocaría hacer un ajuste significativo en la BD".

---

## 5. Convenciones y decisiones de diseño confirmadas

- **Naming de campos**: las entidades JPA usan camelCase mapeado a snake_case vía `@Column(name = "...")`; no se sigue el nombre literal de la columna SQL cuando conviene semántica de negocio distinta (ver nota histórica: en el ejemplo original de `Cuestionario`, el campo Java se llamaba `nombre` pese a que la columna SQL es `descripcion` — para las tablas nuevas del Grupo C se prefirió respetar el nombre real de columna, ej. `numeroIdentificacion` para `numero_identificacion`).
- **`Identificable`/`Nombrable`**: solo se implementan cuando la entidad tiene una clave de negocio real de tipo nombre único. No se implementan "por si acaso" aunque Lombok genere `getId()`/`getNombre()` que técnicamente cumplirían la interfaz — hacerlo sin necesidad sería semántica engañosa y código muerto.
- **Filtrado en listados**: se prefiere `@RequestParam` opcional sobre `GET` general (ej. `GET /evaluados?numeroIdentificacion=123`, `GET /evaluado-clientes?fkEvaluado=X&fkCliente=Y`) en vez de rutas anidadas tipo `GET /evaluados/cedula/{id}`, para mantener consistencia con el patrón ya usado en Grupo B.
- **Búsqueda por clave de negocio devuelta como lista de 1 elemento**: cuando un filtro por `@RequestParam` encuentra coincidencia única (ej. por cédula), se envuelve en `List.of(resultado)` para mantener el tipo de retorno consistente (`List<Response>`) entre el caso filtrado y el caso de listar todo. Alternativa no tomada: usar `ResponseEntity<?>` o endpoints separados.
- **Violaciones de FK**: no se valida manualmente en cada service que las FKs referenciadas existan antes de guardar; se delega en la constraint de Postgres + el handler genérico de `DataIntegrityViolationException` en `GlobalExceptionHandler`. Decisión tomada para no repetir la misma validación en las ~9 tablas de Grupo C restantes.
- **`evaluado_cliente`**: representa la relación laboral evaluado↔cliente. Solo puede existir **una fila activa** (`activo = true`) por par (`fk_evaluado`, `fk_cliente`). Si el evaluado deja la empresa y años después vuelve a ser contratado por el mismo cliente, se permite crear una **fila nueva** (no se reactiva la anterior) — se conserva el historial completo.

---

## 6. Incidentes resueltos en esta sesión (para no repetir)

1. **Flyway checksum mismatch en `V1`**: ocurrió porque se editó `V1__initial_schema.sql` después de haber sido aplicado (se eliminaron los `INSERT` de datos maestros). Como los datos ya estaban insertados en la BD, la solución correcta fue `flyway:repair` (no `DROP SCHEMA`, que habría dejado los catálogos vacíos). Comando final que funcionó en PowerShell (los argumentos `-D` deben ir entre comillas por el `://` en la URL):
   ```powershell
   ./mvnw flyway:repair "-Dflyway.url=jdbc:postgresql://localhost:5432/bateria_psicosocial" "-Dflyway.user=postgres" "-Dflyway.password=1234"
   ```
   **Regla a futuro**: nunca editar una migración de Flyway ya aplicada; crear una migración nueva (`V2__...`) en su lugar.

2. **`DataIntegrityViolationException` al actualizar `cliente`**: fue un error de uso (se llamó `PUT` con `fkIndustria=0` en vez de un ID válido), no un bug — confirmó que el handler agregado en `GlobalExceptionHandler` funciona correctamente devolviendo 409 en vez de 500.

---

## 7. Estado de `application.properties`

```properties
spring.application.name=riesgo-psicosocial
server.port=8080
server.servlet.context-path=/api
spring.profiles.active=dev
spring.jpa.open-in-view=false
spring.jackson.time-zone=America/Bogota
spring.jackson.serialization.write-dates-as-timestamps=false
spring.flyway.repair-on-migrate=false
logging.config=classpath:logback-spring.xml
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
```

`application-dev.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bateria_psicosocial
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.time_zone=America/Bogota
```

(`ddl-auto=validate` → el esquema lo gestiona Flyway exclusivamente; Hibernate solo valida que las entidades coincidan.)

---

## 8. Preguntas abiertas / pendientes antes de continuar

1. **`ErrorCode.DATA_INTEGRITY_VIOLATION`**: ✅ confirmado, ya está agregado al enum `ErrorCode`.
2. **`aplicacion.estado`**: ¿existe un catálogo cerrado de valores (`FINALIZADA`, `EN_PROGRESO`, `ANULADA`...) o es texto libre por ahora? Sin resolver aún.
3. **Diseño de Grupo D — `respuesta`**: ✅ resuelto (ver sección 2). Pendiente solo el motor de cálculo (`resultado_*`):
   - Definir el disparador del cálculo (automático al finalizar `aplicacion` vs. endpoint explícito)
   - Diseñar el `domain`/`application` del motor de cálculo de riesgo

---

## 9. Script SQL completo de referencia

El script de creación de las ~34 tablas (`cuestionario`, `dominio`, `dimension`, `opcion_respuesta`, `escala`, `escala_detalle`, `nivel_riesgo`, `dimension_cuestionario`, `dominio_cuestionario`, `baremo_dimension`, `baremo_dominio`, `baremo_cuestionario`, `pregunta`, tablas maestras de evaluado/cliente, ubicación geográfica, sectores/industria, `cliente`, `evaluado`, `evaluado_cliente`, `aplicacion`, `respuesta`, `resultado_dimension`, `resultado_dominio`, `resultado_cuestionario`) ya fue compartido al inicio de la conversación original y corresponde a `V1__initial_schema.sql`. Pégalo aparte en IntelliJ si necesitas que Claude lo tenga en contexto de nuevo, ya que no se incluye aquí para no duplicar un archivo grande que ya tienes en el repo.

---

## 10. Siguiente paso acordado

Motor de cálculo implementado (ver sección 2). Pendientes: corregir preguntas de forma C, cargar baremos totales de C/D, ítems condicionales de A/B y puntaje total general. Diseño original del motor: `resultado_dimension`, `resultado_dominio`, `resultado_cuestionario`, en `domain`/`application` (hexagonal completo), incluyendo:
- El disparador del cálculo (automático al finalizar la `aplicacion` vs. endpoint explícito).
- La lógica de suma de `puntaje_bruto` a partir de `respuesta` agrupado por `dimension_cuestionario`/`dominio_cuestionario`/`cuestionario`.
- La aplicación de `factor_transformacion` para obtener `puntaje_transformado`.
- La búsqueda del `nivel_riesgo` correspondiente según los rangos (`valor_minimo`/`valor_maximo`) de `baremo_dimension`/`baremo_dominio`/`baremo_cuestionario`.
