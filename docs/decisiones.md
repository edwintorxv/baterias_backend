# Registro de decisiones

Este documento reúne las decisiones de diseño y de negocio que no se deducen del código, o que podrían parecer un error a quien llega nuevo al proyecto. Cada una dice **qué se decidió**, **por qué** y, cuando aplica, **qué se descartó**.

**Antes de cambiar algo de esta lista, hay que hablarlo con el responsable del proyecto.** Casi todas son pedidos explícitos, no detalles de implementación.

Para agregar una decisión: va en su tema, con la fecha (`AAAA-MM-DD`) y el motivo. Si reemplaza a otra, la anterior no se borra: se marca como *reemplazada* y se enlaza la nueva.

## Arquitectura y código

**Hexagonal parcial.** Solo el motor de cálculo y los informes tienen dominio puro con puertos y adaptadores; el resto es CRUD directo `Controller → Service → JpaRepository`.
- *Motivo*: ahí está la lógica de la batería y conviene probarla sin base de datos. En el CRUD, los puertos solo agregarían capas.
- Ver `docs/arquitectura.md`.

**Auditoría manual**, sin `@EnableJpaAuditing` ni listeners. Cada servicio llena `usuario_crea`, `usuario_modifica` y `fecha_modificado`.
- *Motivo*: preferencia del responsable; queda explícito en cada servicio qué se audita.
- *Consecuencia*: un servicio nuevo debe hacerlo a mano. Además, cada `PUT` marca "modificado" aunque no cambie nada.

**Sin `DELETE` en las tablas de negocio.** Los registros se inactivan (`activo`, `fecha_retiro`).
- *Motivo*: trazabilidad, y porque los informes pasados dependen de esos datos.

**Las FK no se validan en Java.** Las detiene PostgreSQL y el manejador global responde 409.
- *Motivo*: evita una consulta por cada referencia; la base ya garantiza la integridad.

**Datos maestros por migración (V16, idempotente); datos de prueba solo en la base local.**
- *Motivo*: una base vacía queda lista con solo arrancar. Los datos de prueba tienen información de personas y no van a git.
- *Consecuencia*: si cambia un dato maestro, va en una migración nueva, no editando la V16.

**Todo en hora de Colombia** (`America/Bogota`): JVM, Jackson, Hibernate y la sesión de PostgreSQL.
- *Motivo*: los usuarios están en Colombia y los informes se cortan por año calendario local.

**Código, mensajes, comentarios y commits en español.**
- *Motivo*: el dominio y los usuarios están en español; los mensajes de validación salen siempre en español (`spring.web.locale=es` fijo).

## Batería y cálculo

**2026-10-20 · La forma (A o B) la elige el psicólogo** al crear la aplicación. Si no coincide con el tipo de cargo registrado, el sistema solo avisa; no bloquea.
- *Motivo*: el manual asigna la forma por el cargo real, que el psicólogo conoce mejor que el dato registrado.
- *Descartado*: derivarla siempre del tipo de cargo.

**2026-10-20 · Solo se calcula una aplicación completa**: todas las preguntas que aplican de todos los cuestionarios del grupo.
- *Motivo*: evitar el error humano, porque un cálculo con faltantes podría tomarse como definitivo.
- *Consecuencia*: la tolerancia del manual (1 ítem sin respuesta en cuatro dimensiones) sigue en la calculadora, pero no se alcanza.

**2026-10-20 · Corregir se permite.** Si cambian las respuestas, la forma o las preguntas filtro, los resultados se borran y la aplicación queda pendiente de cálculo.
- *Descartado*: bloquear la edición de una aplicación calculada.

**El estado de la aplicación se calcula, no se guarda** (`EstadoAplicacion`). En la V21 se borró el texto libre `aplicacion.estado`.
- *Motivo*: un estado guardado se desincroniza; calculado, el listado y el formulario siempre dicen lo mismo.

**Pregunta filtro sin registrar = la dimensión aplica.**
- *Motivo*: no dejar de calificar algo por una omisión al digitar.

**Los cuestionarios se distinguen por `metodo_calculo`, nunca por la letra.**
- *Motivo*: la letra es una etiqueta; el método define cómo se califica.

## Personas y empresas

**2026-10-19 · Persona vs. relación (V17).** `evaluado` guarda solo lo que no cambia. Los datos laborales y los personales que cambian son una foto por vinculación en `evaluado_cliente`, con `fecha_vinculacion` y `fecha_retiro`.
- *Motivo*: una persona puede pasar por varias empresas, y cada evaluación debe reflejar sus datos de ese momento.

**2026-10-19 · Una sola relación activa por persona.** Vincularla a otra empresa cierra la anterior. **Una relación cerrada no se edita**: conserva la foto con que se evaluó.

**Vincular es la única forma de crear personas** (`POST /clientes/{id}/personas`). Se quitaron `POST /evaluados` y `POST /evaluado-clientes`.
- *Motivo*: un solo camino, con las mismas reglas, para el formulario y para la carga por archivo.

**El mensaje de "cambia de empresa" no dice cuál era la empresa anterior.**
- *Motivo*: confidencialidad entre clientes.

**La plantilla Excel de carga siempre se descarga vacía**: nunca se exportan personas ya cargadas.
- *Motivo*: protección de datos personales.

**El NIT del cliente no se modifica** después de creado.
- *Motivo*: identifica a la empresa en los informes y en los nombres de archivo.

## Informes

**2026-10-20 · El informe individual lo ve cualquier psicólogo asignado al cliente**, no solo el firmante. El ADMIN y el CLIENTE no lo ven.
- *Motivo*: es historia clínica ocupacional, pero el trabajo se reparte entre los psicólogos del cliente.

**Participantes del año con corte** (31-dic, u hoy si el año no ha terminado). Cuenta quien se evaluó en el año y quien estaba vigente al corte.
- *Motivo*: que el informe de un año pasado no cambie porque la gente se mueva después.

**2026-10-15 · `MINIMO_PERSONAS_GRUPO = 2`.** Una ciudad o área con menos personas se junta en "Otras…" o se omite. Valor **provisional**.
- *Motivo*: que no se pueda identificar el resultado de una persona.

**Ranking sin sumar niveles**: primero el de mayor % en *muy alto*, luego en *alto*, y así hacia abajo.
- *Motivo*: sumar alto y muy alto esconde dónde está el riesgo más grave.

**Presentación**: cada análisis nuevo va como **sección propia al final**, en tabla + barras agrupadas separadas por nivel. Los gráficos son nativos de Word, editables por el psicólogo.
- *Descartado*: mapas de calor y barras 100 % apiladas.

**Los ítems de estrés no se numeran en el Word.**
- *Motivo*: van ordenados por frecuencia, y la numeración quedaría salteada.

**La periodicidad (Res. 2764, art. 3) se calcula al vuelo.** No se guarda, no compara años y no da una fecha de próxima evaluación.

**La descarga del informe de empresa exige conclusiones y las tres acciones.**
- *Motivo*: que no salga un informe sin la parte profesional.

**Descartado: PDF del informe de empresa.** Solo Word, que el psicólogo ajusta.

**Descartado: las preguntas al área usuaria**: factor protector/riesgo, mínimo N oficial, catálogo de áreas y consolidado de varias empresas.

**2026-10-29 · El PDF del individual se deja como está.** Si queda mal repartido entre páginas, el psicólogo ajusta el Word.

## Seguridad y usuarios

**JWT propio, sin sesión en el servidor. El rol y el estado activo se leen de la BD en cada petición**, no del token.
- *Motivo*: inactivar a alguien o cambiarle el rol aplica de inmediato, sin esperar a que venza el token.

**La tabla de permisos por rol la aprobó el responsable.** Una ruta no clasificada queda denegada.
- *Consecuencia*: el ADMIN **no ve informes ni datos de trabajadores**.

**Lo ajeno responde 404, no 403.**
- *Motivo*: no revelar qué clientes, personas o aplicaciones existen.

**2026-10-20 · Firma**: solo el propio evaluador o el ADMIN la ven o la suben. El psicólogo solo puede ponerse a sí mismo como firmante. Las firmas no tienen historial: el informe usa la vigente al generarse.

**2026-10-21 · Los evaluadores (los demás psicólogos) solo los ve el ADMIN.** El psicólogo gestiona la suya en "Mi firma".

**2026-10-21 · Se ingresa con el correo (V20).**
- *Motivo*: evita usuarios duplicados y deja listo el envío de correos.

**2026-10-28 · Registro de ingresos en una tabla** (`registro_ingreso`), con retención de 12 meses.
- *Motivo*: se puede consultar y depurar. La retención es porque la IP es un dato personal.
- *Descartado*: solo el log.

**Freno a la fuerza bruta en memoria.**
- *Motivo*: hay una sola instancia, así que basta.
- *Consecuencia*: se pierde al reiniciar, y con varias instancias habría que moverlo.

**2026-10-29 · Contraseñas por enlace, no por correo.**
- *Motivo*: una contraseña enviada quedaría en la bandeja y en los registros de Brevo.
- *Consecuencia*: el ADMIN nunca ve ni escribe contraseñas. Se quitó `PUT /usuarios/{id}/contrasena`.

**2026-10-29 · Sin "¿Olvidó su contraseña?".** Quien la olvida llama **por teléfono** al administrador, y él genera el enlace. En ese momento:
- la contraseña actual deja de servir;
- se cierran sus sesiones;
- se anulan sus enlaces anteriores.

El enlace vence a las 48 h y es de un solo uso.
- *Motivo*: la verificación de identidad la hace una persona; el sistema no ofrece pedir enlaces.

**2026-11-06 · "Cambiar mi contraseña" cierra las demás sesiones y devuelve un token nuevo.** La contraseña actual cuenta para el freno del login.

**El rol EVALUADO no se crea por ahora.**
- *Motivo*: está previsto para que el trabajador responda en línea a futuro.

**2026-10-21 · Las ayudas de desarrollo del front se quedan hasta producción**: selector de usuario de prueba, "entrar como", auto-login y reingreso ante 401. Ya no salen en el build de producción; queda revisar si se quitan del código.

## Correo

**Brevo por API HTTP, no SMTP.** El correo sale después del commit, en segundo plano y sin reintentos.
- *Motivo*: el mismo enfoque probado en otro proyecto. Un fallo de Brevo no debe romper la operación.

**2026-11-06 · El remitente es una cuenta de Brevo compartida con otro proyecto, con nombre "PsicoBatería" y clave de API propia.**

**Modo log sin clave.**
- *Motivo*: desarrollar sin enviar correos.
- *Consecuencia*: en producción hay que verificar que las variables estén puestas.

## Infraestructura

**2026-10-27 · Render (backend con Docker + front estático) y Supabase, en planes gratuitos.** Render despliega la rama `despliegue`.
- *Motivo*: costo cero mientras el sistema se prueba. La rama aparte permite subir trabajo a `main` sin publicarlo.

**2026-10-28 · Región Virginia (us-east)**, junto a Supabase us-east-1.
- *Motivo*: menor latencia con la base. La lentitud que queda es la CPU del plan Free.

**2026-11-24 · La infraestructura se deja como está** (Render Free + Supabase Free). El sistema funciona bien para el uso actual.
- *Se aceptan*: el arranque de ≈ 2,5 min tras dormirse y la pausa de Supabase tras 7 días sin actividad, que se resuelve con *Restore project*.
- Ver `docs/despliegue.md`.

## Datos y documentación

**2026-10-29 · Los datos locales son la demo para reuniones.** Nada de scripts que llenen datos: todo se carga por la pantalla. El respaldo `.dump` se guarda fuera del repo.

**2026-11-24 · Documentación técnica para desarrolladores**, en Markdown dentro de `docs/`, versionada con el código. El detalle de cada endpoint queda en Swagger.

## Por decidir

| Tema | Estado |
|---|---|
| Dimensión de C "Tiempo fuera del trabajo" vs. "Balance entre la vida laboral y familiar" (Anexo 2) | Lo decide el psicólogo |
| Consentimiento informado, custodia/retención de la historia clínica, registro de quién consulta o descarga informes individuales | Propuesto el 2026-10-21; por validar con el área psicológica/legal |
| Plan de Render (Starter o mantenerlo despierto) y pausa de Supabase | Se deja como está (2026-11-24); se retoma si el uso lo exige |
| `MINIMO_PERSONAS_GRUPO` definitivo | Valor provisional 2 |
| `UNIQUE (nit)` en `cliente` (hoy la unicidad la valida solo el servicio) | Hallazgo del 2026-11-24 |
| Borrar las columnas viejas de `evaluado` que pasaron a la relación (V17) | Pendiente de una migración |
