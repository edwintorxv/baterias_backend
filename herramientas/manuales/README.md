# Manual técnico en Word

Genera **un documento Word por repositorio**: uno del backend y otro del front. Cada uno incluye el `README.md` del repo (capítulo 1, puesta en marcha) y los manuales de su `docs/`, con portada, índice, encabezado y número de página.

**Los `.md` son la fuente.** El Word nunca se edita a mano: si cambia un manual, se vuelve a generar.

```bash
cd herramientas/manuales
npm install              # la primera vez (node_modules no va a git)
npm run generar          # → ~/proyectos/Documentacion/baterias/Manual técnico/Manual técnico PsicoBatería – Backend.docx
npm run generar:front    # → ~/proyectos/Documentacion/baterias/Manual técnico/Manual técnico PsicoBatería – Frontend.docx
npm run generar -- --salida /otra/ruta/manual.docx
npm run generar:front -- --raiz /otra/ruta/baterias_front
```

- **Proyectos**: están en `PROYECTOS`, al inicio de `generar.mjs`, cada uno con su carpeta, su subtítulo y sus capítulos en orden. El front se busca al lado del backend, con la misma organización de carpetas (`proyectos/backend/baterias_backend` y `proyectos/frontend/baterias_front`); si está en otro lugar, se indica con `--raiz`. Un manual nuevo en `docs/` se agrega a la lista de su proyecto.
- **Diagramas**: los bloques ```` ```mermaid ```` se convierten a PNG en el equipo con `@mermaid-js/mermaid-cli`. Nada sale del equipo. Usa el Chromium de Puppeteer (`~/.cache/puppeteer`); si no está, `npm install` lo descarga (~150 MB). Un `flowchart LR` demasiado ancho se dibuja de arriba abajo para que la letra se pueda leer. Mermaid 11 toma `@` como sintaxis: en un diagrama se escribe `#64;`.
- **Enlaces** a otro `.md` del mismo repo (`docs/seguridad.md`, `seguridad.md`) se vuelven referencias internas al capítulo («capítulo 6, «Seguridad»»). Una ruta de otro repo (`baterias_backend/docs/seguridad.md`) queda como texto.
- El `.docx` se guarda **fuera del repo**: es binario y se regenera.

Para revisar las páginas sin Word, se puede abrir el `.docx` en ONLYOFFICE, Pages o Google Docs.

## Manuales de usuario (por rol)

Además de los técnicos, la herramienta genera **un manual de usuario por rol** (Psicólogo, Cliente, Administrador), con capturas de pantalla. Todo vive **fuera de los repos**, en `~/proyectos/Documentacion/baterias/Manuales de usuario/`:

```
Manuales de usuario/
├── manual-psicologo.md, manual-cliente.md, manual-administrador.md   ← la fuente (cada "# Título" es un capítulo)
├── capturas/<rol>/NN-nombre.jpg                                         ← las capturas
└── Manual del <Rol> – PsicoBatería.docx                                 ← el Word generado
```

```bash
npm install
npm run capturas -- --rol psicologo        # o cliente / administrador → capturas/<rol>/
npm run generar:psicologo                  # o generar:cliente / generar:administrador → el .docx
```

- **Capturas** (`capturas.mjs`): necesitan el front (`http://localhost:4201`) y el backend (`http://localhost:8080`) corriendo. Entran con los usuarios de prueba de `environment.development.ts` del front (`--front` si el repo está en otra carpeta) y recorren las pantallas del rol con Puppeteer.
  - **Datos ficticios**: todas las respuestas de la API se cambian antes de llegar al navegador (personas, documentos, empresas, NIT, contactos, correos, usuarios, IP, tarjeta y licencia) y las **firmas** (imágenes) por un trazo de ejemplo. La base no se toca.
  - **Red de seguridad**: antes de cada captura se busca en la pantalla cada valor original; si aparece alguno, la imagen **no se guarda** y el script lo avisa. Las imágenes no se revisan así (por eso las firmas se reemplazan siempre): revisar las capturas a ojo antes de publicar un manual.
  - **Solo mira**: cualquier petición que no sea GET (salvo el login) detiene el script; no guarda ni envía nada (el backend local envía correos de verdad).
  - Oculta las ayudas de desarrollo (selector de usuario, "Entrar como", sección Desarrollo del Inicio).
- **Word**: la portada (versión con `--version`, fecha, "Documento interno. No publicar."), el índice, las secciones numeradas (1.1, 1.2…), las figuras (`![pie de figura](capturas/rol/archivo.jpg)` → "Figura N. pie de figura") y los recuadros (`> **Importante:** …`) salen del `.md`.
- Si cambia una pantalla: volver a correr las capturas de los roles que la ven y regenerar sus Word.
