// Genera el Manual técnico (Word) a partir de README.md y docs/*.md.
// Los .md son la fuente: el Word nunca se edita a mano, se vuelve a generar.
//
//   npm install            (una vez)
//   npm run generar        → ~/proyectos/Documentacion/baterias/Manual técnico/
//   npm run generar -- --salida /otra/ruta/manual.docx
//
// Los diagramas Mermaid se convierten a PNG en el equipo con mermaid-cli (nada sale del equipo).

import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { marked } from 'marked';
import {
    AlignmentType, Bookmark, BorderStyle, Document, ExternalHyperlink, Footer, Header, HeadingLevel, ImageRun,
    InternalHyperlink, LevelFormat, Packer, PageNumber, Paragraph, ShadingType, Table, TableCell, TableRow,
    TextRun, WidthType,
} from 'docx';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(AQUI, '..', '..');

/** Capítulos en orden: archivo y título del capítulo (el H1 del .md se reemplaza por este). */
const CAPITULOS = [
    { archivo: 'README.md', titulo: 'Introducción y puesta en marcha' },
    { archivo: 'docs/arquitectura.md', titulo: 'Arquitectura' },
    { archivo: 'docs/modelo-de-datos.md', titulo: 'Modelo de datos' },
    { archivo: 'docs/motor-de-calculo.md', titulo: 'Motor de cálculo' },
    { archivo: 'docs/informes.md', titulo: 'Informes' },
    { archivo: 'docs/seguridad.md', titulo: 'Seguridad' },
    { archivo: 'docs/correo.md', titulo: 'Correo' },
    { archivo: 'docs/despliegue.md', titulo: 'Despliegue' },
    { archivo: 'docs/decisiones.md', titulo: 'Registro de decisiones' },
];

const TITULO = 'Manual técnico PsicoBatería';
const SUBTITULO = 'Backend';
const SALIDA_POR_DEFECTO = path.join(os.homedir(), 'proyectos', 'Documentacion', 'baterias', 'Manual técnico',
    'Manual técnico PsicoBatería – Backend.docx');

// Estilo: azul de la marca, carta, márgenes de 1".
const AZUL = '1E3A8A';
const AZUL_CLARO = 'E8EDF7';
const GRIS_CODIGO = 'F3F4F6';
const GRIS_BORDE = 'BFC5D2';
const FUENTE = 'Arial';
const FUENTE_CODIGO = 'Menlo';
const ANCHO_PAGINA = 12240;
const ALTO_PAGINA = 15840;
const MARGEN = 1440;
const ANCHO_CONTENIDO = ANCHO_PAGINA - 2 * MARGEN; // 9360 DXA = 6,5"
const ANCHO_CONTENIDO_PX = 624; // 6,5" a 96 ppp

// ---------------------------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------------------------

function argumento(nombre) {
    const i = process.argv.indexOf(nombre);
    return i >= 0 ? process.argv[i + 1] : undefined;
}

/** Id de marcador a partir de un texto (Word: letras, números y _; máximo 40, empieza por letra). */
function marcador(texto) {
    const limpio = texto.normalize('NFD').replace(/[̀-ͯ]/g, '')
        .replace(/[^A-Za-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
    return ('m_' + limpio).slice(0, 40);
}

/** Capítulo al que apunta una ruta como "docs/seguridad.md", "seguridad.md" o "README.md". */
function capituloDe(ruta) {
    const base = path.basename(ruta.split('#')[0]);
    return CAPITULOS.findIndex(c => path.basename(c.archivo) === base);
}

function decodificar(texto) {
    return texto.replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>').replace(/&amp;/g, '&');
}

/** Ancho y alto de un PNG (cabecera IHDR). */
function tamanoPng(buffer) {
    return { ancho: buffer.readUInt32BE(16), alto: buffer.readUInt32BE(20) };
}

// ---------------------------------------------------------------------------------------------
// Diagramas Mermaid → PNG
// ---------------------------------------------------------------------------------------------

const temporal = fs.mkdtempSync(path.join(os.tmpdir(), 'manuales-'));
const configMermaid = path.join(temporal, 'config.json');
fs.writeFileSync(configMermaid, JSON.stringify({
    theme: 'base',
    themeVariables: {
        fontFamily: 'Arial', primaryColor: AZUL_CLARO.replace(/^/, '#'), primaryBorderColor: '#' + AZUL,
        primaryTextColor: '#111827', lineColor: '#4B5563', secondaryColor: '#F1F5F9', tertiaryColor: '#FFFFFF',
    },
}));
let numeroDiagrama = 0;
const listasNumeradas = []; // una referencia por lista numerada, para que cada una empiece en 1

let diagramas = 0;

function diagrama(codigo) {
    diagramas++;
    let png = dibujar(codigo);
    let { ancho, alto } = tamanoPng(png);
    if (ancho / alto > 4 && /^\s*flowchart\s+LR/m.test(codigo)) {
        png = dibujar(codigo.replace(/^(\s*flowchart\s+)LR/m, '$1TD'));
        ({ ancho, alto } = tamanoPng(png));
    }
    // Escala 3: el tamaño natural es ancho/3; nunca más ancho que la página ni más alto que ~8,5".
    let w = Math.min(ANCHO_CONTENIDO_PX, ancho / 3);
    let h = alto * w / ancho;
    if (h > 820) { h = 820; w = ancho * h / alto; }
    return new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 120, after: 200 },
        children: [new ImageRun({ type: 'png', data: png, transformation: { width: Math.round(w), height: Math.round(h) } })],
    });
}

function dibujar(codigo) {
    numeroDiagrama++;
    const entrada = path.join(temporal, `d${numeroDiagrama}.mmd`);
    const salida = path.join(temporal, `d${numeroDiagrama}.png`);
    fs.writeFileSync(entrada, codigo);
    const mmdc = path.join(AQUI, 'node_modules', '.bin', 'mmdc');
    execFileSync(mmdc, ['-i', entrada, '-o', salida, '-s', '3', '-b', 'white', '-c', configMermaid, '-q'],
        { stdio: ['ignore', 'ignore', 'inherit'] });
    return fs.readFileSync(salida);
}

// ---------------------------------------------------------------------------------------------
// Markdown → docx
// ---------------------------------------------------------------------------------------------

/** Texto inline (negrita, cursiva, código, enlaces) → runs. */
function inline(tokens, estilo = {}) {
    const runs = [];
    for (const t of tokens ?? []) {
        switch (t.type) {
            case 'text':
            case 'escape':
                if (t.tokens?.length) runs.push(...inline(t.tokens, estilo));
                else runs.push(new TextRun({ text: decodificar(t.text), ...estilo }));
                break;
            case 'strong':
                runs.push(...inline(t.tokens, { ...estilo, bold: true }));
                break;
            case 'em':
                runs.push(...inline(t.tokens, { ...estilo, italics: true }));
                break;
            case 'del':
                runs.push(...inline(t.tokens, { ...estilo, strike: true }));
                break;
            case 'codespan':
                runs.push(...codigoEnLinea(decodificar(t.text), estilo));
                break;
            case 'link':
                runs.push(enlace(t, estilo));
                break;
            case 'br':
                runs.push(new TextRun({ text: '', break: 1 }));
                break;
            case 'html':
                break; // no hay HTML en los .md
            default:
                runs.push(new TextRun({ text: decodificar(t.raw ?? ''), ...estilo }));
        }
    }
    return runs;
}

/** `docs/x.md` en código se vuelve una referencia al capítulo; el resto, monoespaciado. */
function codigoEnLinea(texto, estilo) {
    const cap = /\.md$/.test(texto) ? capituloDe(texto) : -1;
    if (cap >= 0) return [referenciaCapitulo(cap, estilo)];
    if (estilo.color === 'FFFFFF') return [new TextRun({ text: texto, font: FUENTE_CODIGO, ...estilo })]; // encabezado de tabla
    return [new TextRun({ text: texto, font: FUENTE_CODIGO, size: 18, color: '1F2937', ...estilo,
        shading: { type: ShadingType.CLEAR, fill: GRIS_CODIGO, color: 'auto' } })];
}

function referenciaCapitulo(cap, estilo) {
    return new InternalHyperlink({
        anchor: marcador(`cap ${cap + 1} ${CAPITULOS[cap].titulo}`),
        children: [new TextRun({ text: `capítulo ${cap + 1}, «${CAPITULOS[cap].titulo}»`, ...estilo, color: AZUL, underline: {} })],
    });
}

function enlace(t, estilo) {
    const cap = t.href.endsWith('.md') || t.href.includes('.md#') ? capituloDe(t.href) : -1;
    if (cap >= 0) {
        return new InternalHyperlink({
            anchor: marcador(`cap ${cap + 1} ${CAPITULOS[cap].titulo}`),
            children: inline(t.tokens, { ...estilo, color: AZUL, underline: {} }),
        });
    }
    return new ExternalHyperlink({ link: t.href, children: inline(t.tokens, { ...estilo, color: AZUL, underline: {} }) });
}

class Convertidor {
    constructor(numeroCapitulo) {
        this.cap = numeroCapitulo;
        this.secciones = []; // H2 del capítulo, para el índice
    }

    bloques(tokens, contexto = {}) {
        const salida = [];
        for (const t of tokens) salida.push(...this.bloque(t, contexto));
        return salida;
    }

    bloque(t, contexto) {
        switch (t.type) {
            case 'heading': return [this.titulo(t)];
            case 'paragraph': return [this.parrafo(inline(t.tokens), contexto, t.text.trim().endsWith(':') ? { keepNext: true } : {})];
            case 'text': return [this.parrafo(inline(t.tokens ?? [{ type: 'text', text: t.text }]), contexto)];
            case 'list': return this.lista(t, contexto, 0);
            case 'table': return [this.tabla(t), new Paragraph({ spacing: { after: 80 }, children: [] })];
            case 'code': return t.lang === 'mermaid' ? [diagrama(t.text)] : [this.codigo(t.text, contexto)];
            case 'blockquote': return this.bloques(t.tokens, { ...contexto, cita: true });
            case 'space':
            case 'hr':
            case 'html':
                return [];
            default:
                return [this.parrafo([new TextRun(decodificar(t.raw ?? ''))], contexto)];
        }
    }

    titulo(t) {
        const texto = decodificar(t.text).replace(/`/g, '');
        const nivel = { 2: HeadingLevel.HEADING_2, 3: HeadingLevel.HEADING_3 }[t.depth] ?? HeadingLevel.HEADING_4;
        const id = marcador(`cap ${this.cap} ${texto}`);
        if (t.depth === 2) this.secciones.push({ texto, id });
        return new Paragraph({ heading: nivel, children: [new Bookmark({ id, children: inline(t.tokens) })] });
    }

    parrafo(runs, contexto, extra = {}) {
        return new Paragraph({ children: runs, ...this.estiloCita(contexto), ...extra });
    }

    estiloCita(contexto) {
        if (!contexto.cita) return {};
        return {
            indent: { left: 240, right: 120 },
            shading: { type: ShadingType.CLEAR, fill: AZUL_CLARO, color: 'auto' },
            border: { left: { style: BorderStyle.SINGLE, size: 18, color: AZUL, space: 8 } },
        };
    }

    lista(t, contexto, nivel) {
        let referencia = 'vinetas';
        if (t.ordered) {
            referencia = `numerada_${listasNumeradas.length + 1}`;
            listasNumeradas.push({ referencia, inicio: Number(t.start) || 1 });
        }
        const salida = [];
        for (const item of t.items) {
            let primero = true;
            for (const sub of item.tokens) {
                if (sub.type === 'list') {
                    salida.push(...this.lista(sub, contexto, nivel + 1));
                } else if (sub.type === 'text' || sub.type === 'paragraph') {
                    const runs = inline(sub.tokens ?? [{ type: 'text', text: sub.text }]);
                    salida.push(this.parrafo(runs, contexto, primero
                        ? { numbering: { reference: referencia, level: nivel } }
                        : { indent: { left: 720 * (nivel + 1) } }));
                    primero = false;
                } else {
                    salida.push(...this.bloque(sub, contexto));
                }
            }
        }
        return salida;
    }

    codigo(texto, contexto) {
        const lineas = texto.split('\n');
        const runs = [];
        lineas.forEach((linea, i) => runs.push(new TextRun({ text: linea, font: FUENTE_CODIGO, size: 16, break: i > 0 ? 1 : 0 })));
        return new Paragraph({
            children: runs,
            spacing: { before: 80, after: 160, line: 260 },
            shading: { type: ShadingType.CLEAR, fill: GRIS_CODIGO, color: 'auto' },
            border: {
                top: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 },
                bottom: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 },
                left: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 },
                right: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 },
            },
            indent: contexto.cita ? { left: 240 } : { left: 80, right: 80 },
            keepLines: lineas.length <= 25,
        });
    }

    tabla(t) {
        const columnas = t.header.length;
        const conEncabezado = t.header.some(h => h.text.trim() !== '');
        const anchos = this.anchosColumnas(t);
        const celda = (tokens, i, encabezado) => new TableCell({
            width: { size: anchos[i], type: WidthType.DXA },
            shading: encabezado ? { type: ShadingType.CLEAR, fill: AZUL, color: 'auto' } : undefined,
            margins: { top: 60, bottom: 60, left: 100, right: 100 },
            children: [new Paragraph({
                spacing: { before: 0, after: 0, line: 252 },
                alignment: t.align[i] === 'center' ? AlignmentType.CENTER : t.align[i] === 'right' ? AlignmentType.RIGHT : AlignmentType.LEFT,
                children: inline(tokens, encabezado ? { bold: true, color: 'FFFFFF', size: 18 } : { size: 18 }),
            })],
        });
        const borde = { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE };
        return new Table({
            width: { size: ANCHO_CONTENIDO, type: WidthType.DXA },
            columnWidths: anchos,
            borders: { top: borde, bottom: borde, left: borde, right: borde, insideHorizontal: borde, insideVertical: borde },
            rows: [
                ...(conEncabezado ? [new TableRow({ tableHeader: true, cantSplit: true, children: t.header.map((h, i) => celda(h.tokens, i, true)) })] : []),
                ...t.rows.map(fila => new TableRow({ cantSplit: true, children: fila.slice(0, columnas).map((c, i) => celda(c.tokens, i, false)) })),
            ],
        });
    }

    /** Ancho de cada columna según su texto más largo (con mínimo y tope), sumando el ancho de la página. */
    anchosColumnas(t) {
        const largo = (c) => Math.min(60, Math.max(4, decodificar(c.text).replace(/[`*]/g, '').length));
        const pesos = t.header.map((h, i) => Math.max(largo(h), ...t.rows.map(f => (f[i] ? largo(f[i]) : 4))));
        const total = pesos.reduce((a, b) => a + b, 0);
        const anchos = pesos.map(p => Math.max(900, Math.round(ANCHO_CONTENIDO * p / total)));
        const exceso = anchos.reduce((a, b) => a + b, 0) - ANCHO_CONTENIDO;
        const mayor = anchos.indexOf(Math.max(...anchos));
        anchos[mayor] -= exceso;
        return anchos;
    }
}

// ---------------------------------------------------------------------------------------------
// Documento
// ---------------------------------------------------------------------------------------------

function capitulo(indice) {
    const { archivo, titulo } = CAPITULOS[indice];
    const md = fs.readFileSync(path.join(RAIZ, archivo), 'utf8');
    const tokens = marked.lexer(md);
    const primerH1 = tokens.findIndex(t => t.type === 'heading' && t.depth === 1);
    if (primerH1 >= 0) tokens.splice(primerH1, 1);
    // En el README, la sección que enlaza los documentos sobra: el índice del manual ya los lista.
    if (archivo === 'README.md') {
        const i = tokens.findIndex(t => t.type === 'heading' && t.text === 'Documentación técnica');
        if (i >= 0) tokens.splice(i);
    }
    const numero = indice + 1;
    const conv = new Convertidor(numero);
    const cuerpo = conv.bloques(tokens);
    const encabezado = new Paragraph({
        heading: HeadingLevel.HEADING_1,
        pageBreakBefore: true,
        children: [new Bookmark({ id: marcador(`cap ${numero} ${titulo}`), children: [new TextRun(`${numero}. ${titulo}`)] })],
    });
    return { titulo, numero, secciones: conv.secciones, parrafos: [encabezado, ...cuerpo] };
}

function portada() {
    const hoy = new Date().toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
    let version = '';
    try {
        version = execFileSync('git', ['-C', RAIZ, 'log', '-1', '--format=%h'], { encoding: 'utf8' }).trim();
    } catch { /* sin git */ }
    const linea = (texto, opciones = {}) => new Paragraph({ alignment: AlignmentType.CENTER, ...opciones.p, children: [new TextRun({ text: texto, font: FUENTE, ...opciones.r })] });
    return [
        new Paragraph({ spacing: { before: 2600 }, children: [] }),
        linea('PsicoBatería', { r: { size: 28, color: AZUL, bold: true }, p: { spacing: { after: 240 } } }),
        linea('Manual técnico', { r: { size: 56, bold: true, color: '111827' }, p: { spacing: { after: 120 } } }),
        linea(SUBTITULO, { r: { size: 36, color: '4B5563' }, p: {
            spacing: { after: 600 },
            border: { bottom: { style: BorderStyle.SINGLE, size: 12, color: AZUL, space: 16 } },
        } }),
        linea('Sistema de aplicación y calificación de la Batería de Instrumentos para la Evaluación de Factores de Riesgo Psicosocial', { r: { size: 22, color: '374151' }, p: { spacing: { after: 120 } } }),
        linea('Documentación técnica para desarrolladores', { r: { size: 22, color: '374151' }, p: { spacing: { after: 1800 } } }),
        linea(hoy, { r: { size: 20, color: '4B5563' } }),
        linea(version ? `Generado desde los .md del repositorio · versión ${version}` : 'Generado desde los .md del repositorio', { r: { size: 16, color: '6B7280' }, p: { spacing: { before: 80 } } }),
    ];
}

function indice(capitulos) {
    const salida = [new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: true, children: [new TextRun('Contenido')] })];
    for (const c of capitulos) {
        salida.push(new Paragraph({
            spacing: { before: 160, after: 40 },
            children: [new InternalHyperlink({ anchor: marcador(`cap ${c.numero} ${c.titulo}`),
                children: [new TextRun({ text: `${c.numero}. ${c.titulo}`, bold: true, color: AZUL, size: 24 })] })],
        }));
        for (const s of c.secciones) {
            salida.push(new Paragraph({
                indent: { left: 400 }, spacing: { before: 0, after: 20 },
                children: [new InternalHyperlink({ anchor: s.id, children: [new TextRun({ text: s.texto, size: 20, color: '374151' })] })],
            }));
        }
    }
    return salida;
}

function documento(capitulos) {
    const nivelesLista = (formato, texto) => [0, 1, 2].map(nivel => ({
        level: nivel, format: formato, text: texto(nivel), alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 360 * (nivel + 1) + 0, hanging: 300 } } },
    }));
    const encabezadoTitulo = (size, color, before, after) => ({
        run: { font: FUENTE, size, bold: true, color },
        paragraph: { spacing: { before, after }, keepNext: true, keepLines: true },
    });
    return new Document({
        creator: 'PsicoBatería',
        title: `${TITULO} – ${SUBTITULO}`,
        description: 'Documentación técnica del backend, generada desde README.md y docs/*.md',
        styles: {
            default: {
                document: { run: { font: FUENTE, size: 21, color: '1F2937' }, paragraph: { spacing: { after: 120, line: 276 } } },
                heading1: encabezadoTitulo(36, AZUL, 0, 240),
                heading2: encabezadoTitulo(28, AZUL, 360, 120),
                heading3: encabezadoTitulo(24, '111827', 240, 80),
                heading4: encabezadoTitulo(22, '374151', 200, 60),
            },
        },
        numbering: {
            config: [
                { reference: 'vinetas', levels: nivelesLista(LevelFormat.BULLET, n => ['•', '◦', '▪'][n]) },
                ...listasNumeradas.map(l => ({ reference: l.referencia,
                    levels: nivelesLista(LevelFormat.DECIMAL, n => `%${n + 1}.`).map(v => ({ ...v, start: v.level === 0 ? l.inicio : 1 })) })),
            ],
        },
        sections: [
            {
                properties: { page: { size: { width: ANCHO_PAGINA, height: ALTO_PAGINA }, margin: { top: MARGEN, bottom: MARGEN, left: MARGEN, right: MARGEN } } },
                children: portada(),
            },
            {
                properties: { page: { size: { width: ANCHO_PAGINA, height: ALTO_PAGINA }, margin: { top: MARGEN, bottom: MARGEN, left: MARGEN, right: MARGEN }, pageNumbers: { start: 1 } } },
                headers: { default: new Header({ children: [new Paragraph({
                    alignment: AlignmentType.RIGHT,
                    border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 } },
                    children: [new TextRun({ text: `${TITULO} – ${SUBTITULO}`, size: 16, color: '6B7280' })],
                })] }) },
                footers: { default: new Footer({ children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    children: [new TextRun({ children: ['Página ', PageNumber.CURRENT], size: 16, color: '6B7280' })],
                })] }) },
                children: [...indice(capitulos).map((p, i) => (i === 0 ? new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun('Contenido')] }) : p)),
                    ...capitulos.flatMap(c => c.parrafos)],
            },
        ],
    });
}

// ---------------------------------------------------------------------------------------------

try {
    const salida = path.resolve(argumento('--salida') ?? SALIDA_POR_DEFECTO);
    const capitulos = CAPITULOS.map((_, i) => capitulo(i));
    const buffer = await Packer.toBuffer(documento(capitulos));
    fs.mkdirSync(path.dirname(salida), { recursive: true });
    fs.writeFileSync(salida, buffer);
    // Sin entradas de carpeta en el zip: Word ha rechazado archivos que las traen al abrirlos desde la nube.
    try { execFileSync('zip', ['-dq', salida, '*/'], { stdio: 'ignore' }); } catch { /* no había o no hay zip */ }
    console.log(`Manual generado: ${salida}`);
    console.log(`  ${capitulos.length} capítulos, ${diagramas} diagramas`);
} finally {
    fs.rmSync(temporal, { recursive: true, force: true });
}
