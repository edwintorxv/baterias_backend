# Informes

El sistema entrega dos informes:

| | Informe individual | Informe de empresa |
|---|---|---|
| De quién | Un trabajador en una empresa | Una empresa en un año |
| Para quién | El psicólogo: es parte de la **historia clínica ocupacional** del trabajador | El psicólogo y el cliente (la empresa), este en solo lectura |
| Formatos | JSON, **PDF** y **Word** | JSON y **Word** |
| Base | Los resultados de sus aplicaciones (ver `docs/motor-de-calculo.md`) | Los resultados de todas las personas de la empresa en el año |

Los dos son **hexágono** (ver `docs/arquitectura.md`): el análisis está en el dominio y la carga de datos y la generación de archivos son adaptadores. **Ninguno se guarda.** Se arman al vuelo cada vez que se piden. Solo se guarda lo que escribe el psicólogo:

- Informe individual: observaciones, recomendaciones y firmante, en `aplicacion`.
- Informe de empresa: conclusiones, plan de acción y firmante, en `informe_empresa`.

El **año** de un informe es el año de `aplicacion.fecha_aplicacion`.

## Informe individual

### Endpoints (solo PSICOLOGO, con alcance por cliente)

| Endpoint | Devuelve |
|---|---|
| `GET /clientes/{idCliente}/evaluados/{idEvaluado}/informe?anio=` | JSON; sin `anio` trae el historial completo |
| `GET .../informe/anios` | Años con aplicaciones, del más reciente al más antiguo |
| `GET .../informe/archivo?anio=&formato=pdf\|docx` | Archivo `informe_{documento}_{anio}.{ext}` |

Las observaciones, recomendaciones y firmante se guardan aparte, con `PUT /aplicaciones/{id}`. El front lo hace desde el diálogo del informe.

### Contenido

Sigue los formatos modelo del manual: Anexos 4 y 5 para el intralaboral y Anexo 2 para el extralaboral. Estrés usa la misma estructura. Hay **un archivo por año** y, dentro, una sección por aplicación con:

- Encabezado "CONFIDENCIAL – Hace parte de la historia clínica ocupacional del trabajador".
- Datos del trabajador, con la foto de la vinculación (ver *Persona vs. relación* en `docs/modelo-de-datos.md`) y la edad al año de la aplicación.
- Datos del evaluador.
- Resultados por cuestionario: total, dominios y dimensiones con su puntaje transformado y su nivel. Para estrés, solo el total.
- **Síntomas de estrés frecuentes**: los ítems de D respondidos *Siempre* o *Casi siempre*, agrupados por dimensión en el orden del cuestionario (*Siempre* primero y, en empate, por número de pregunta). No llevan puntaje ni nivel, porque el manual no trae baremos por dimensión de estrés. Esto lo resuelve `SeleccionSintomasFrecuentes`.
- Interpretación de los niveles, observaciones, recomendaciones y la **firma** del evaluador.

### Reglas

- Para descargar el archivo, cada aplicación del año debe tener **resultados calculados** y **evaluador asignado**: el manual exige los datos del evaluador. Si falta algo, responde 409 con la lista de lo que falta. El JSON sí sale aunque falten.
- Si el evaluador no tiene firma cargada, el informe sale sin firma. El front solo avisa.
- La firma usada es la **vigente al generar el informe**; las firmas no tienen historial.
- **Confidencialidad**: lo ve cualquier psicólogo asignado al cliente, no solo el firmante. El ADMIN no lo ve. El CLIENTE tampoco, porque es historia clínica.

### Flujo

```
InformeController
  → ConsultarInformeEvaluadoUseCase / GenerarDocumentoInformeEvaluadoUseCase
  → InformeEvaluadoService            arma InformeEvaluado (dominio)
  → DocumentoInformeEvaluadoService   valida, carga las firmas, elige el generador por formato
  → GeneradorDocumentoInformePort
       ├─ PdfInformeEvaluadoGenerator   Thymeleaf (templates/informes/informe-evaluado.html) → OpenHTMLtoPDF
       └─ DocxInformeEvaluadoGenerator  Apache POI
```

El PDF y el Word salen del mismo modelo de vista, `InformeVista`, que construye `InformeVistaMapper`. Así los dos formatos muestran lo mismo. Si el PDF queda mal repartido entre páginas, el psicólogo puede ajustar el Word.

## Informe de empresa

### Endpoints

| Endpoint | Rol | Devuelve |
|---|---|---|
| `GET /clientes/{idCliente}/informe?anio=` | PSICOLOGO, CLIENTE | JSON |
| `GET .../informe/anios` | PSICOLOGO, CLIENTE | Años con aplicaciones |
| `GET .../informe/archivo?anio=&formato=docx` | PSICOLOGO, CLIENTE | `informe_empresa_{nit}_{anio}.docx` |
| `GET .../informe/conclusiones?anio=` | PSICOLOGO, CLIENTE | Conclusiones y plan de acción |
| `PUT .../informe/conclusiones?anio=` | PSICOLOGO | Guarda conclusiones, acciones y firmante |

El PDF del informe de empresa no existe: `formato=pdf` responde 409. Fue una decisión descartar ese formato.

### Quién cuenta

Esta es la regla más importante del informe, porque garantiza que **el informe de un año pasado no cambie aunque la gente se mueva después**.

- **Participantes del año**: quien se evaluó en el año, aunque hoy esté retirado o en otra empresa, y quien estaba vigente al **corte**. El corte es el 31 de diciembre o, si el año no ha terminado, hoy (`PersonaCargada.vigenteAl` / `corteDelAnio`).
- **Esperados** = participantes. **Evaluados** = participantes con respuestas en alguna aplicación del año.
- Cada persona cuenta **una sola vez**, con su aplicación más reciente que tenga resultados.
- Una persona *pendiente de cálculo* cuenta como evaluada (cobertura), pero no entra en las tablas de resultados.
- Área, ciudad y nivel académico salen de la **relación**, es decir, la foto de la vinculación.

### Estructura del Word

La numeración de las secciones es automática según las formas presentadas:

1. Datos de la empresa.
2. **Introducción**: marco de la Res. 2764 de 2022, objetivo, instrumentos aplicados, población, periodicidad y confidencialidad.
3. **Población evaluada**: cobertura por ciudad y características de las personas (forma, sexo, rango de edad, nivel académico, tipo de cargo y tipo de contrato).
4. **Resultados globales**: intralaboral con el **nivel de la empresa y la periodicidad**, extralaboral, total general y estrés.
5. **Una sección por forma (A / B)**: ranking de dominios, ranking de dimensiones y fichas de cada dominio.
6. **Concentración del riesgo intralaboral (A + B)**: por dominio, ciudad y área.
7. **Factores extralaborales**: por dimensión, ciudad y área.
8. **Síntomas de estrés**: frecuencia por dimensión, ciudad y área.
9. **Síntomas de estrés por ítem**: una tabla y gráficos de a dos síntomas por dimensión.
10. Interpretación de los niveles.
11. **Conclusiones**.
12. **Plan de acción** (corto, mediano y largo plazo) y profesional responsable con su firma.

> **Convención para análisis nuevos**: cada uno va como **sección propia al final**, salvo que se pida otra ubicación. Se presenta como tabla + barras agrupadas separadas por nivel. No se usan mapas de calor ni barras 100 % apiladas.

### Reglas de análisis (`AnalizadorInformeEmpresa`)

**Porcentajes y rankings**
- Los porcentajes van a 1 decimal (HALF_UP), sobre el total de **cada tabla**: solo cuentan quienes tienen resultado de ese cuestionario.
- **Ranking sin sumar niveles** (`DistribucionNiveles.masCriticaPrimero()`): primero va el de mayor % en *muy alto*; en empate, el de mayor % en *alto*, y así hacia abajo. Si empatan en todo, se respeta el orden del cuestionario.

**Cómo se cuenta a cada persona**
- **Intralaboral junta A y B**: cada persona presenta una sola forma, y su nivel ya sale del baremo de esa forma. En la concentración del riesgo, los dominios de A y B se cruzan por el dominio común.
- **Dimensiones condicionales**: no cuentan a quien respondió "no" a la pregunta filtro. Su puntaje 0 significa que no aplica, no que no haya riesgo.

**Agrupaciones por ciudad y área**
- **Áreas**: se agrupan sin distinguir mayúsculas, tildes ni espacios repetidos, y se muestran con la escritura más frecuente. Sin área = "Sin área".
- **Ciudades**: se agrupan por id, porque hay municipios con el mismo nombre. Sin ciudad = "Sin ciudad".
- **Confidencialidad de grupos pequeños** (`MINIMO_PERSONAS_GRUPO = 2`): una ciudad o área con menos personas no se muestra sola, porque su resultado sería el de una persona identificable. Esos grupos se juntan en "Otras ciudades" u "Otras áreas" al final; si aun juntos no llegan al mínimo, se omiten con una nota.

**Síntomas de estrés**
- **Por dimensión**: % de respuestas *Siempre* … *Nunca* sobre personas × ítems, mostrado como "632 (158 × 4)". Solo cuentan las respuestas de la aplicación de donde sale el resultado de estrés de cada persona, para que coincida con el total.
- **Por ítem** (`SintomasEstres.porItem`): las dimensiones van en el orden del cuestionario (fisiológicos primero) y los síntomas del más frecuente al menos frecuente, con el mismo criterio sin sumar y, en empate, por número de pregunta. La base son las personas que respondieron el ítem. **El Word no numera los ítems**, porque al ir ordenados la numeración quedaría salteada; el JSON trae `numero` solo como referencia.

### Nivel de la empresa y periodicidad

La **Res. 2764 de 2022, art. 3**, fija cada cuánto se repite la evaluación según el nivel de riesgo intralaboral (`AnalizadorInformeEmpresa.nivelRiesgoEmpresa`). El cálculo es:

1. Por cada forma presentada (A y/o B), se promedia el **puntaje bruto total** intralaboral de sus personas.
2. Ese promedio se transforma con el factor de la forma (`CalculadoraResultado.transformar`) y se ubica en el baremo total de esa forma.
3. La periodicidad es **anual** si alguna forma queda en riesgo alto o muy alto, y **cada dos años** en caso contrario (enum `Periodicidad`).

La regla está en código; los factores y baremos vienen de la BD. El resultado se calcula al vuelo: no se guarda, no se compara entre años y no se calcula una fecha de la próxima evaluación.

### Conclusiones y descarga

`informe_empresa` guarda una fila por cliente y año con las **conclusiones**, las **acciones a corto, mediano y largo plazo** y el **evaluador** que firma (opcional). Su CRUD es `ConclusionesInformeEmpresaService`.

- **La descarga del Word exige las cuatro partes de texto**: conclusiones y las tres acciones. Si falta alguna, responde 409 con la lista de lo que falta.
- El JSON trae `conclusiones.faltantes` para que el front habilite el botón de descarga.
- Con evaluador asignado, el Word pone sus datos y su firma.

### Errores

| Situación | HTTP |
|---|---|
| Cliente inexistente o ajeno | 404 |
| Año sin aplicaciones | 404 |
| Cliente sin personas cargadas | 409 |
| Formato no soportado (`pdf`) | 409 |
| Faltan conclusiones o acciones al descargar | 409 |

### Flujo

```
InformeEmpresaController
  → ConsultarInformeEmpresaUseCase / GenerarDocumentoInformeEmpresaUseCase
  → InformeEmpresaService
      ├─ InformeEmpresaPort → InformeEmpresaPersistenceAdapter   carga en bloque: personas, aplicaciones con
      │                       (+ FichasPersonaLoader, SintomasEstresLoader)   resultados, respuestas de estrés, configuración
      └─ AnalizadorInformeEmpresa (dominio)                       cobertura, población, rankings, concentración,
                                                                  extralaboral, estrés, nivel de la empresa
  → InformeEmpresa (dominio)
      ├─ InformeEmpresaResponseMapper        → JSON (sin cálculos: solo traduce)
      └─ DocumentoInformeEmpresaService      → DocxInformeEmpresaGenerator → .docx
```

El JSON y el Word salen **del mismo `InformeEmpresa`**, así que siempre coinciden. La pantalla de Estadísticas del front usa ese mismo JSON.

## Generación de archivos

| Pieza | Qué hace |
|---|---|
| `DocxSoporte` | Piezas comunes a los dos Word: estilos, tablas, celdas con color por nivel y firma |
| `DocxGraficos` | **Gráficos nativos de Word**, editables por el psicólogo: barras agrupadas de a 2 grupos, 6 cm de alto, título dinámico y sin etiquetas en 0 % |
| `DocxInformeEmpresaGenerator` | Word del informe de empresa |
| `DocxInformeEvaluadoGenerator` | Word del informe individual |
| `PdfInformeEvaluadoGenerator` | PDF del individual: Thymeleaf → HTML → OpenHTMLtoPDF |
| `InformeVista` / `InformeVistaMapper` | Modelo de vista común al PDF y al Word del individual |

- **Colores por nivel**: el informe de empresa usa `COLOR_NIVEL` (semáforo) y el individual `COLOR_NIVEL_SUAVE`.
- **Un formato nuevo** se agrega implementando `GeneradorDocumentoInformePort` o `GeneradorDocumentoInformeEmpresaPort`. El servicio arma un mapa de generadores por `FormatoDocumento`, así que no hay que tocarlo.
- La respuesta del archivo expone `Content-Disposition` para que el front tome el nombre (ver CORS en `docs/seguridad.md`).

## Tests

| Qué | Tests |
|---|---|
| Dominio (sin base de datos) | `AnalizadorInformeEmpresaTest`, `SeleccionSintomasFrecuentesTest`, `PersonaCargadaTest`, `ConclusionesInformeTest` |
| Servicios | `InformeEvaluadoServiceTest`, `DocumentoInformeEvaluadoServiceTest`, `InformeEmpresaServiceTest`, `DocumentoInformeEmpresaServiceTest`, `ConclusionesInformeEmpresaServiceTest` |
| Generadores | `GeneradoresInformeEvaluadoTest` (PDF y Word del individual), `DocxInformeEmpresaGeneratorTest` |
| JSON | `InformeEmpresaResponseMapperTest` |

**Si se cambia una regla del informe de empresa**, hay que verificar que el JSON, el Word y las Estadísticas del front sigan cuadrando.
