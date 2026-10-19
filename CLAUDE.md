# Contexto — Sistema de Baterías de Riesgo Psicosocial (backend)

> Resumen para retomar el desarrollo. Última actualización: 2026-10-18. Todo commiteado y en `origin/main`.
> Forma de trabajo: **paso a paso** (diseño → código → el usuario revisa → commit/push cuando lo pide).

## 0. Puesta en marcha

- **JDK 17**, **PostgreSQL 16**, `./mvnw` (en macOS: `sh mvnw`). Repo `https://github.com/edwintorxv/baterias_backend.git`, rama `main`.
- BD `bateria_psicosocial` (postgres/1234, `application-dev.properties`). API `http://localhost:8080/api`, Swagger `/api/swagger-ui.html`.
- **Los datos maestros NO están en las migraciones** (catálogos, cuestionarios, preguntas, escalas, baremos y datos de prueba viven solo en la BD local). En otra máquina: `pg_dump -F c` + `pg_restore` (trae `flyway_schema_history`; el `.dump` tiene datos de evaluados: **no subirlo a git**). Pendiente: migración `V16__datos_maestros.sql` (V14 y V15 ya se usaron) con los datos ya corregidos (incluidas las comas de la V14) y `setval` de secuencias.
- **La app la arranca el usuario en IntelliJ (8080)**: no levantar/detener instancias sin avisar. Tiene **spring-boot-devtools**: un `mvnw compile`/`test` la reinicia sola y Flyway aplica migraciones nuevas en la BD local; `@SpringBootTest` también corre contra la BD local. Avisar antes de compilar si hay una migración nueva.
- **Dependencia nueva en el `pom.xml`** → devtools no la carga y la app queda caída: el usuario recarga Maven y reinicia en IntelliJ (avisarle).
- Tests: `sh mvnw test` → **123** (dominio, servicios con fakes/Mockito, generadores Word/PDF, mapper JSON, seguridad con `@WebMvcTest`, ArchUnit, contexto).
- Manuales y Res. 2764 de 2022 en PDF (fuera del repo): `/Users/edwingacha/proyectos/Documentacion/baterias/` (texto en macOS con `osascript -l JavaScript` + PDFKit). El `.pptx` de ejemplo de informe de empresa tiene datos reales: no subirlo.

## 1. Stack y arquitectura

Spring Boot 3.5.16, Java 17, PostgreSQL, Flyway (`ddl-auto=validate`), MapStruct, Lombok, Apache POI 5.4.1, OpenHTMLtoPDF + Thymeleaf. `context-path=/api`, zona `America/Bogota`.

**Hexagonal parcial**: hexágono completo solo en el motor de cálculo (`resultado`) y los informes (`informe`); el resto es CRUD `Controller → Service → JpaRepository`.
```
domain/            lógica pura (sin Spring/JPA/shared); lanza domain.exception.ReglaNegocioException (→ 409)
application/       port/in (casos de uso), port/out, service
infrastructure/adapter/in/rest/{modulo}/(dto/)       controladores + records
infrastructure/adapter/out/persistence/{tabla}/      Entity + JpaRepository (+ Service si es CRUD)
infrastructure/adapter/out/documento/informe/        generadores Word/PDF
shared/            catalogo (CRUD genérico), exception, response (ApiResponse/ApiError/ResponseBuilder)
```
Reglas ArchUnit (`ArquitecturaHexagonalTest`): domain solo `java..`; application sin infrastructure/JPA/Spring Data; puertos solo con modelos del dominio; adaptadores de entrada usan `port.in` (los CRUD quedan fuera a propósito).

**Moldes CRUD**: (a) *catálogo simple* (`CatalogoCrudService<T extends Identificable & Nombrable>` + `CatalogoJpaRepository`) solo si hay un `nombre` único real; (b) *entidad plana* (Entity, `JpaRepository` directo, Service propio con su validación de unicidad, Controller, Request/Response records).

**Convenciones**: camelCase ↔ snake_case con `@Column`; filtros por `@RequestParam` opcional (búsqueda única devuelta como `List.of(x)`); FKs inválidas las ataja Postgres + handler `DataIntegrityViolationException` → 409; sin `DELETE` en tablas de negocio; auditoría (`fecha_creado` por DEFAULT, el resto manual) sin `@EnableJpaAuditing` (decisión del usuario).

**Errores** (`GlobalExceptionHandler`): 404 `ResourceNotFoundException`; 409 `BusinessException`/`ReglaNegocioException`/integridad; 400 validación, parámetro faltante o de tipo inválido; 500 el resto.

**Regla Flyway**: nunca editar una migración aplicada (incidente: checksum de V1 → `flyway:repair`); si se cargan datos con ids explícitos, terminar con `setval`.

## 2. Dominio de la batería y motor de cálculo

- Cuestionarios: A y B intralaboral (`SUMA_POR_DOMINIOS`, factores 492 y 388), C extralaboral (`SUMA_DIRECTA`, 124), D estrés (`PROMEDIO_PONDERADO`, 61,16; pesos por dimensión 4/3/2/1). Se distinguen por `metodo_calculo`, nunca por letra. C y D son iguales para todos; lo que cambia por grupo ocupacional es el baremo.
- `grupo_ocupacional` (1 jefes/profesionales/técnicos → A; 2 auxiliares/operarios → B). `aplicacion.fk_grupo_ocupacional` es **foto al aplicar** (se deriva de `tipo_cargo` si no viene).
- `respuesta`: inmutable (sin PUT); `valor_obtenido` lo calcula el backend con `escala_detalle`.
- **Motor** (`POST/GET /aplicaciones/{id}/resultados`, recalculable): `CalculadoraResultado` (dominio puro). Transformado = bruto/factor×100 a 1 decimal HALF_UP (`CalculadoraResultado.transformar`, pública). A/B guardan dimensiones+dominios+total; C dimensiones+total; D solo total (el manual no trae baremos por dimensión de D). Falta un baremo → 409, nunca resultados parciales.
- Preguntas filtro (V7/V8): `aplicacion.atiende_clientes`/`es_jefe` (`NULL` = no registrado); `dimension_cuestionario.condicion_aplicacion` (demandas emocionales, relación con los colaboradores) y `max_items_sin_respuesta` (tolerancia 1 en 4 dimensiones de A/B). Dimensión que no aplica = bruto 0.
- **Total general** (V9): bruto A o B + bruto C, factor 616 (A+C) o 512 (B+C), baremos Tabla 34 en `baremo_total_general`; `resultado_total_general`. D no entra.
- Etiquetas de estrés: mismos `nivel_riesgo` 1–5 mostrados como Muy bajo…Muy alto (`domain.model.resultado.NivelEstres`), título "Nivel de síntomas de estrés".
- Toda la configuración (escalas, ítems, factores, dominios, dimensiones y baremos de A, B, C y D) quedó verificada contra los manuales (V3–V6).

**Migraciones**: V1 esquema · V2 grupo ocupacional/método de cálculo · V3 escalas invertidas e ítems de C · V4 baremos de C y D · V5 secuencias · V6 retroalimentación A/B · V7 preguntas filtro · V8 condiciones y tolerancias · V9 total general · V10 evaluador · V11 guion U+0096 · V12 errata "Bachillerato incompleto" · V13 `informe_empresa` (conclusiones) · V14 comas que habían quedado como puntos en 13 preguntas (A, B y D) · V15 `usuario` + `usuario_cliente`. Siguiente libre: **V16**.

## 3. Informe individual — ✅ completo y verificado por el usuario (Word y PDF)

- `GET /clientes/{idCliente}/evaluados/{idEvaluado}/informe?anio=` (JSON; sin `anio` = historial), `.../informe/anios`, `.../informe/archivo?anio=&formato=pdf|docx`.
- Sigue los formatos modelo de la batería (Anexos 4 y 5 intralaboral, Anexo 2 extralaboral; D con la estructura de los demás). Un archivo por año, una sección por aplicación; datos del trabajador y del evaluador, resultados, interpretación, observaciones, recomendaciones y firma. Encabezado "CONFIDENCIAL – Hace parte de la historia clínica ocupacional del trabajador".
- Aplicación sin evaluador o sin resultados → 409. PDF y Word salen de `InformeVista` (`InformeVistaMapper`).
- **Evaluador** (V10): CRUD `/evaluadores` (sin DELETE, `activo`), firma por `PUT /evaluadores/{id}/firma` (PNG/JPG ≤ 1 MB). Solo se asignan evaluadores activos; quien ya lo tenía lo conserva aunque se inactive.
- El informe individual es del trabajador (historia clínica): para el psicólogo, no para el cliente.

## 4. Informe de empresa — ✅ completo (Word y JSON)

Endpoints: `GET /clientes/{idCliente}/informe?anio=` (JSON), `.../informe/anios`, `.../informe/archivo?anio=&formato=docx` (`informe_empresa_{nit}_{anio}.docx`), `GET/PUT .../informe/conclusiones?anio=`.

**Orden del Word** (numeración automática según las formas presentadas): datos de la empresa · 1 Introducción (marco Res. 2764, objetivo, instrumentos aplicados, población, periodicidad, confidencialidad) · 2 Población evaluada (cobertura por ciudad + características: forma, sexo, rango de edad, nivel académico, tipo de cargo y de contrato) · 3 Resultados globales (intralaboral + **nivel de la empresa y periodicidad**, extralaboral, total general, estrés) · una sección por forma A/B (ranking de dominios, ranking de dimensiones, fichas) · Concentración del riesgo intralaboral A+B (dominios, ciudad, área) · Factores extralaborales (dimensiones, ciudad, área) · Síntomas de estrés (frecuencia por dimensión, ciudad, área) · Síntomas de estrés por ítem (una tabla + gráficos de a 2 síntomas por dimensión, sin ciudad ni área) · Interpretación de los niveles · Conclusiones · Plan de acción (corto/mediano/largo) + profesional responsable con firma.

**Reglas**
- Esperados = relaciones activas de `evaluado_cliente`; evaluados = con respuestas en el año. Cada persona cuenta una vez, con su aplicación más reciente con resultados. "Pendiente de cálculo" cuenta como evaluada pero no entra en las tablas.
- % a 1 decimal sobre el total de cada tabla. **Ranking sin sumar niveles**: primero el mayor % en muy alto, luego alto… (`DistribucionNiveles.masCriticaPrimero()`).
- Dimensiones condicionales: no cuentan a quien respondió "no" a la pregunta filtro.
- Área y ciudad: valor **actual** de `evaluado_cliente`; áreas sin distinguir mayúsculas/tildes/espacios.
- **Confidencialidad**: `MINIMO_PERSONAS_GRUPO = 2` (decisión del usuario); por prueba, grupos menores se juntan en "Otras ciudades/áreas" al final; si aun juntos no llegan, se omiten con nota.
- Estrés por dimensión: % de respuestas Siempre…Nunca (personas × ítems, mostrado como "632 (158 × 4)").
- Estrés por ítem (`SintomasEstres.porItem`, JSON `sintomasEstres.porItem`): dimensiones en el orden del cuestionario (fisiológicos primero); síntomas del más frecuente al menos (mismo criterio sin sumar, empate por número de pregunta); base = personas que respondieron el ítem. **Sin numeración de ítems en el Word** (al ir ordenados quedaría salteada; el JSON trae `numero` solo como referencia).
- **Periodicidad (Res. 2764 de 2022, art. 3)**: por forma, promedio del bruto total intralaboral → transformado → baremo de la forma; **anual** si alguna forma queda en alto/muy alto, si no **cada dos años**. Regla en código (`AnalizadorInformeEmpresa.nivelRiesgoEmpresa`, enum `Periodicidad`), datos en BD (factores, `baremo_cuestionario`, `nivel_riesgo`). Se calcula al vuelo (no se guarda, no compara años, no da fecha).
- **Conclusiones** (V13, `informe_empresa`, una fila por cliente+año): conclusiones, tres acciones y `fk_evaluador` opcional. **La descarga del Word exige las cuatro partes** (409 con lo que falta); el JSON trae `conclusiones.faltantes` para que el front habilite la descarga. Con evaluador, el Word pone sus datos y su firma.
- Errores: cliente inexistente / año sin aplicaciones → 404; cliente sin personas → 409; formato no soportado (pdf) → 409.

**Diseño**: dominio `domain/model/informe/empresa/` + `domain/service/informe/AnalizadorInformeEmpresa` (puro); `InformeEmpresaService` / `DocumentoInformeEmpresaService` / `InformeEmpresaPort`; `InformeEmpresaPersistenceAdapter` (carga en bloque) + `FichasPersonaLoader`; Word `DocxInformeEmpresaGenerator` + `DocxSoporte` (piezas comunes a los dos informes, incluida la firma) + `DocxGraficos` (gráficos nativos de Word editables; barras agrupadas de a 2 grupos, 6 cm, título dinámico, sin etiquetas en 0 %); JSON `InformeEmpresaResponseMapper` (sin cálculo, sale del mismo `InformeEmpresa` que el Word). Colores semáforo `COLOR_NIVEL` (empresa); el individual usa `COLOR_NIVEL_SUAVE`.

**Preferencias del usuario para informes**: cada análisis nuevo va como **sección propia al final** (salvo que pida otra ubicación); tabla + barras agrupadas separadas por nivel (no mapas de calor ni barras 100 % apiladas); recordar la lista de pasos con su estado antes de proponer el siguiente. Descartados: PDF del informe de empresa y las preguntas al área usuaria (factor protector/riesgo, mínimo N oficial, catálogo de áreas, consolidado de varias empresas).

**Datos de prueba (BD local)**: cliente 1 "CALZADO CARDINS", 200 personas, 162 evaluadas en 2026 (aplicaciones 196, 197 y 200 sin calcular a propósito); evaluador 1 "Psicóloga de Prueba" activo con firma. Nivel de la empresa: A 27,8 y B 30,0 (medio) → cada dos años. "EAF" es un área de prueba de texto libre.

## 5. Autenticación — ✅ completa (verificada contra la app con los tres roles)

- **JWT propio** (Spring Security + `oauth2-resource-server`, HS256): `POST /auth/login` → token de 8 h; `GET /auth/yo`; `PUT /auth/contrasena`. Secreto en `APP_SEGURIDAD_JWT_SECRETO` (≥ 32 caracteres; sin él no arranca; solo el perfil dev trae uno). BCrypt. Login fallido: mismo mensaje y mismo tiempo exista o no el usuario.
- **Fuerza bruta** (`LimiteIntentosLogin`, en memoria, una sola instancia): 5 fallos por usuario o 20 por IP → bloqueo de 15 min desde el último fallo (`app.seguridad.login.*`); se revisa antes de BD/BCrypt → **429** `TOO_MANY_REQUESTS` con `Retry-After`. El éxito reinicia solo el usuario, no la IP. Verificado contra la app.
- El rol y `activo` se leen **de la BD en cada petición** (`UsuarioJwtConverter`), no del token. 401/403 en formato `ApiError` (`UNAUTHORIZED`/`FORBIDDEN`). Swagger con "Authorize".
- **Usuarios** (V15): CRUD `/usuarios` (solo ADMIN, sin DELETE), `PUT /usuarios/{id}/contrasena`, `GET/PUT /usuarios/{id}/clientes`. Vínculo por rol: PSICOLOGO → `fk_evaluador` (activo, uno por evaluador), CLIENTE → `fk_cliente`, EVALUADO → `fk_evaluado`, ADMIN sin vínculo. Primer admin: `APP_SEGURIDAD_ADMIN_USUARIO`/`_CONTRASENA` si la tabla está vacía (`PrimerAdministrador`).
- **Permisos por rol** (`SecurityConfig`, tabla aprobada por el usuario; test `PermisosPorRolTest`): ADMIN usuarios, configuración, clientes y evaluadores, **no ve informes ni datos de trabajadores**; PSICOLOGO todo el trabajo e informes; CLIENTE solo el informe de empresa (JSON, Word, conclusiones en lectura); EVALUADO nada (previsto para responder en línea a futuro). **Ruta no clasificada = denegada.**
- **Alcance por cliente** (`AlcanceUsuario` + `AlcanceClienteInterceptor` sobre `/clientes`, `/evaluado-clientes`, `/aplicaciones`, `/respuestas`, `/evaluados`; cuerpos y listados en cada controlador): el psicólogo solo sus clientes asignados, el cliente solo su empresa; lo ajeno responde **404** igual que si no existiera. Evaluado visible si tiene relación con un cliente suyo o ninguna; búsqueda exacta por documento sin filtro (para vincular). Firmante (`fkEvaluador` en aplicaciones y conclusiones): vacío, el propio o el que ya tenía (403 si otro). Firma del evaluador: solo la propia (ADMIN cualquiera).
- **CORS global** (`CorsConfig`, ningún controlador lo declara): orígenes en `APP_CORS_ORIGENES` (coma; admite patrones); sin valor, ninguno; dev acepta cualquier puerto de localhost. Sin credenciales (el token va en `Authorization`); expone `Content-Disposition` para el nombre de los archivos.
- **Auditoría manual** (decisión del usuario, sin listener ni `@EnableJpaAuditing`): cada servicio pone `usuario_crea` al crear y `usuario_modifica` + `fecha_modificado` al actualizar con `UsuarioActual.nombre()` (usuario de la sesión; "sistema" sin sesión). Un servicio nuevo debe hacer lo mismo. Marca "modificado" en cada PUT aunque no cambie nada.
- Usuarios de prueba en la BD local: `admin`, `psicologa` (evaluador 1, cliente 1 asignado), `rrhh.cardins` (CLIENTE del 1). En la BD hay 1.835 evaluados sin relación con ninguna empresa (carga del 2026-09-22): revisar.

## 6. Pendientes

**Antes de producción**
1. Revisar los 1.835 evaluados sin empresa. Al desplegar detrás de un proxy: `server.forward-headers-strategy=native` (si no, el límite por IP ve la del proxy).
2. Migración `V16__datos_maestros.sql` para arrancar sobre una BD vacía.
3. Front (no existe): clientes → informe de empresa por año, o empleados → informe individual; diligenciar conclusiones y las tres acciones antes de descargar el Word de empresa.

**Menores / por decidir**
4. `aplicacion.estado`: ¿catálogo cerrado (FINALIZADA, EN_PROGRESO, ANULADA…) o texto libre?
5. Dimensión de C "Tiempo fuera del trabajo" vs "Balance entre la vida laboral y familiar" (Anexo 2): lo decide el psicólogo.
6. `PUT /aplicaciones` no permite volver `atiendeClientes`/`esJefe` a `NULL` (solo por SQL).
7. Mensajes de bean validation en inglés ("must not be blank").

**Opcionales del informe de empresa**: puntaje promedio del grupo con su nivel por baremo (como el ejemplo); fecha de inicio de la intervención para calcular la próxima evaluación; guardar la periodicidad como registro histórico; evolución entre años; exponer `AnalizadorInformeEmpresa.analizarPorArea`.
