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
