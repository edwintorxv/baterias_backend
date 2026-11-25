# Manual técnico en Word

Genera **un solo documento Word** con el `README.md` del repo (capítulo 1, puesta en marcha) y los manuales de `docs/` (capítulos 2 a 9). Incluye portada, índice, encabezado y número de página.

**Los `.md` son la fuente.** El Word nunca se edita a mano: si cambia un manual, se vuelve a generar.

```bash
cd herramientas/manuales
npm install            # la primera vez (node_modules no va a git)
npm run generar        # → ~/proyectos/Documentacion/baterias/Manual técnico/Manual técnico PsicoBatería – Backend.docx
npm run generar -- --salida /otra/ruta/manual.docx
```

- **Diagramas**: los bloques ```` ```mermaid ```` se convierten a PNG en el equipo con `@mermaid-js/mermaid-cli`. Nada sale del equipo. Usa el Chromium de Puppeteer (`~/.cache/puppeteer`); si no está, `npm install` lo descarga (~150 MB). Un `flowchart LR` demasiado ancho se dibuja de arriba abajo para que la letra se pueda leer.
- **Enlaces** a otro `.md` (`docs/seguridad.md`) se vuelven referencias internas al capítulo («capítulo 6, «Seguridad»»).
- **Capítulos**: el orden y los títulos están en `CAPITULOS`, al inicio de `generar.mjs`. Un manual nuevo en `docs/` se agrega ahí.
- El `.docx` se guarda **fuera del repo**: es binario y se regenera.

Para revisar las páginas sin Word, se puede abrir el `.docx` en ONLYOFFICE, Pages o Google Docs.
