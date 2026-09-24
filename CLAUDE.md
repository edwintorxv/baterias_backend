# Contexto del Proyecto — Sistema de Baterías de Riesgo Psicosocial

> Documento de contexto para continuar el desarrollo desde IntelliJ (Claude Pro / Claude Code).
> Generado a partir de una sesión de diseño e implementación del backend.

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

**Próximo paso acordado**: diseñar e implementar el motor de cálculo de riesgo (`resultado_dimension`, `resultado_dominio`, `resultado_cuestionario`) en `domain`/`application`, incluyendo el disparador del cálculo.

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

Grupo C completo y `respuesta` (Grupo D) implementado (ver sección 2). El siguiente paso es diseñar e implementar el motor de cálculo de riesgo: `resultado_dimension`, `resultado_dominio`, `resultado_cuestionario`, en `domain`/`application` (hexagonal completo), incluyendo:
- El disparador del cálculo (automático al finalizar la `aplicacion` vs. endpoint explícito).
- La lógica de suma de `puntaje_bruto` a partir de `respuesta` agrupado por `dimension_cuestionario`/`dominio_cuestionario`/`cuestionario`.
- La aplicación de `factor_transformacion` para obtener `puntaje_transformado`.
- La búsqueda del `nivel_riesgo` correspondiente según los rangos (`valor_minimo`/`valor_maximo`) de `baremo_dimension`/`baremo_dominio`/`baremo_cuestionario`.
