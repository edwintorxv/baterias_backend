# Correo

El backend envía correos con la **API HTTP de Brevo**, no por SMTP. Hoy hay dos correos, y los dos tienen que ver con contraseñas:

| Correo | Cuándo | Plantilla |
|---|---|---|
| **Enlace de contraseña** | Al crear un usuario activo (invitación) y cuando el admin genera un enlace (restablecer) | `templates/correo/enlace-contrasena.html` |
| **"Su contraseña cambió"** | Al definir la contraseña por el enlace y al cambiarla con "Cambiar mi contraseña" | `templates/correo/contrasena-cambiada.html` |

No se envía nada al crear el primer administrador, que se crea con la contraseña del `.env`. Las reglas de los enlaces están en `docs/seguridad.md`.

## Piezas

```mermaid
flowchart LR
    S[Servicio<br/>EnlaceContrasenaService<br/>UsuarioService] -- "publica evento<br/>(dentro de la transacción)" --> E((Evento))
    E -- "@TransactionalEventListener<br/>después del commit" --> L[CorreoEnlaceContrasena<br/>CorreoContrasenaCambiada]
    L -- "enviar(para, asunto,<br/>plantilla, variables)" --> C[CorreoService<br/>@Async]
    C -- "Thymeleaf:<br/>plantilla dentro de base.html" --> C
    C -- "POST /v3/smtp/email" --> B[Brevo]
```

Todo está en `infrastructure/adapter/out/correo`, salvo los eventos, que están junto a sus servicios en `persistence/usuario`.

| Pieza | Qué hace |
|---|---|
| `EnlaceContrasenaGenerado` | Evento con el correo, el nombre, el propósito, el código en claro y el vencimiento. Su `toString` **no incluye el código**, para que no aparezca si alguien registra el evento en el log |
| `ContrasenaCambiada` | Evento con el correo, el nombre, la fecha y `primeraVez` (si venía de una invitación). **Nunca lleva la contraseña** |
| `CorreoEnlaceContrasena` | Escucha el primer evento, arma el enlace `{APP_FRONT_URL}/definir-contrasena?codigo=…` y el asunto según el propósito |
| `CorreoContrasenaCambiada` | Escucha el segundo evento y arma el aviso |
| `CorreoService` | Procesa la plantilla, la mete en `base.html` y llama a Brevo |

## Decisiones de diseño

- **El correo sale después del commit** (`@TransactionalEventListener`). Si la transacción falla, por ejemplo por un error al guardar el enlace, no se envía un correo con un enlace que no existe. La contracara: si Brevo falla, el cambio en la BD ya quedó hecho.
- **Se envía en segundo plano** (`@Async`, habilitado con `@EnableAsync` en la clase principal). Quien hace la petición no espera a Brevo, y un Brevo lento no demora la respuesta. Usa el ejecutor por defecto de Spring Boot.
- **Un fallo de Brevo no rompe nada.** Se escribe en el log con el código HTTP y la respuesta de Brevo, **sin la clave de la API ni el contenido del correo**. No hay reintentos: si el correo no llega, el admin genera otro enlace.
- **Tiempos máximos**: 5 s para conectar y 15 s para leer (`spring.http.client.*`), para que un envío colgado no ocupe el hilo.
- **Las contraseñas nunca viajan por correo**, solo un enlace de un solo uso. Fue una decisión explícita: una contraseña quedaría guardada en la bandeja del usuario y en los registros de Brevo.

## Modo log

Si falta `BREVO_API_KEY` **o** falta `APP_CORREO_REMITENTE`, el backend **no envía nada**. En su lugar escribe en el log el destinatario, el asunto y las variables, **incluido el enlace**:

```
Correo sin enviar (modo log, falta BREVO_API_KEY o remitente) a ana@ejemplo.com | asunto: ... | variables: {enlace=http://localhost:4201/definir-contrasena?codigo=..., ...}
```

- Sirve para desarrollar sin enviar correos reales: se copia el enlace del log.
- `CorreoService.enviaDeVerdad()` dice en qué modo está. Al generar un enlace en modo log, la respuesta de `POST /usuarios/{id}/enlace-contrasena` lo avisa en el mensaje.
- Si hay clave pero falta el remitente, el backend lo avisa en el log al arrancar.
- **En producción, el modo log es un riesgo**: los enlaces quedarían en los logs de Render. Después de cada despliegue, conviene revisar que el mensaje de "Enlace de contraseña" no diga que está en modo log.

## Configuración

| Variable | Propiedad | Uso |
|---|---|---|
| `BREVO_API_KEY` | `app.correo.brevo-api-key` | Clave de la API (`xkeysib-…`). **No** sirve la clave SMTP (`xsmtpsib-…`) |
| `APP_CORREO_REMITENTE` | `app.correo.remitente` | Correo del remitente. Debe estar verificado en Brevo |
| `APP_CORREO_NOMBRE` | `app.correo.nombre-remitente` | Nombre que se muestra (por defecto "PsicoBatería") |
| `APP_FRONT_URL` | `app.front.url` | Base de los enlaces, sin `/` al final. En dev, `http://localhost:4201`; en producción, `https://baterias-front.onrender.com`. Si falta, el backend avisa al arrancar (`Falta APP_FRONT_URL`) y los enlaces quedan incompletos |

Con una clave real en local, los correos **se envían de verdad**. No generes enlaces para correos inventados (por ejemplo `@baterias.local`): rebotan y dañan la reputación del remitente en Brevo. Para probar, usa un alias de Gmail (`nombre+prueba@gmail.com`).

## Plantillas

Están en `src/main/resources/templates/correo/` y usan Thymeleaf.

- **`base.html`**: el marco común, con el encabezado azul "PsicoBatería", el pie y la variable `cuerpo`, que es la plantilla ya procesada. Los estilos van **en línea**, porque muchos clientes de correo ignoran `<style>`.
- **`enlace-contrasena.html`**:
  - El texto cambia según `invitacion` ("Bienvenido…" o "defina su nueva contraseña").
  - Lleva el botón al enlace y la fecha de vencimiento (48 h).
- **`contrasena-cambiada.html`**:
  - Dice qué pasó, el usuario, la fecha y la hora.
  - Si no es la primera vez, agrega "Las sesiones que tenía abiertas en otros equipos se cerraron".
  - Cierra con "¿No fue usted? Comuníquese de inmediato por teléfono con el administrador".

Las variables se escriben con `th:text`, que las escapa. Solo el `cuerpo` ya armado entra sin escapar en `base.html`.

### Agregar un correo nuevo

1. **Plantilla** `templates/correo/<nombre>.html`: solo el contenido, sin `<html>`, porque `base.html` pone el marco. Estilos en línea.
2. **Evento** (un `record`) que el servicio publica con `ApplicationEventPublisher` dentro de su transacción. Sin datos sensibles, o con un `toString` que los oculte.
3. **Componente que escucha** con `@TransactionalEventListener` y llama a `correoService.enviar(para, asunto, "<nombre>", variables)`.
4. **Test** del componente, con `CorreoService` simulado, que verifique el asunto y las variables. Ver `CorreoEnlaceContrasenaTest` y `CorreoContrasenaCambiadaTest`.

## Tests

| Test | Qué cubre |
|---|---|
| `CorreoServiceTest` | Modo log sin clave o sin remitente (no llama a Brevo), envío simulado con remitente, destinatario y HTML, variables escapadas, y que ni un fallo de Brevo ni una plantilla inexistente lancen excepción |
| `CorreoEnlaceContrasenaTest` | Enlace al front con el código, asunto según el propósito, y plantilla con nombre, botón y vencimiento |
| `CorreoContrasenaCambiadaTest` | Asunto y fecha sin la contraseña, otro asunto la primera vez, y texto según el caso con el aviso de llamar |

## Cuenta de Brevo

- La cuenta se comparte con otro proyecto, pero **cada proyecto tiene su propia clave de API**.
- Si una clave se filtra o deja de funcionar, se genera otra en Brevo (*SMTP & API → API Keys*) y se reemplaza en el `.env` local y en el Secret File de Render (ver `docs/despliegue.md`).
- El plan gratuito de Brevo tiene un límite diario de envíos, que hoy está muy por encima del uso del sistema.
