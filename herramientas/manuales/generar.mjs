// Genera el Manual técnico (Word) a partir de README.md y docs/*.md de un repositorio (backend o front).
// Los .md son la fuente: el Word nunca se edita a mano, se vuelve a generar.
//
//   npm install            (una vez)
//   npm run generar        → ~/proyectos/Documentacion/baterias/Manual técnico/ (backend)
//   npm run generar:front  → ídem, manual del front (repo baterias_front al lado del backend)
//   npm run generar -- --salida /otra/ruta/manual.docx
//   npm run generar:front -- --raiz /otra/ruta/baterias_front
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
const RAIZ_BACKEND = path.resolve(AQUI, '..', '..');
const CARPETA_SALIDA = path.join(os.homedir(), 'proyectos', 'Documentacion', 'baterias', 'Manual técnico');
/** Manuales de usuario: el .md, las capturas (capturas/<rol>/) y el Word, fuera de los repos. */
const CARPETA_USUARIO = path.join(os.homedir(), 'proyectos', 'Documentacion', 'baterias', 'Manuales de usuario');

/**
 * Manuales técnicos: uno por repositorio. Manuales de usuario (`tipo: 'usuario'`): uno por rol, en un solo .md
 * donde cada título `#` es un capítulo; sus secciones se numeran (1.1, 1.2…) y las imágenes llevan "Figura N".
 *
 * Técnicos: un manual por repositorio. `raiz` es la carpeta del repo (el front se busca al lado del backend, con la misma
 * organización de carpetas; `--raiz` la cambia). Capítulos en orden: archivo y título del capítulo (el H1 del .md
 * se reemplaza por este).
 */
const PROYECTOS = {
    backend: {
        raiz: RAIZ_BACKEND,
        subtitulo: 'Backend',
        capitulos: [
            { archivo: 'README.md', titulo: 'Introducción y puesta en marcha' },
            { archivo: 'docs/arquitectura.md', titulo: 'Arquitectura' },
            { archivo: 'docs/modelo-de-datos.md', titulo: 'Modelo de datos' },
            { archivo: 'docs/motor-de-calculo.md', titulo: 'Motor de cálculo' },
            { archivo: 'docs/informes.md', titulo: 'Informes' },
            { archivo: 'docs/seguridad.md', titulo: 'Seguridad' },
            { archivo: 'docs/correo.md', titulo: 'Correo' },
            { archivo: 'docs/despliegue.md', titulo: 'Despliegue' },
            { archivo: 'docs/decisiones.md', titulo: 'Registro de decisiones' },
        ],
    },
    front: {
        raiz: path.resolve(RAIZ_BACKEND, '..', '..', 'frontend', 'baterias_front'),
        subtitulo: 'Frontend',
        capitulos: [
            { archivo: 'README.md', titulo: 'Introducción y puesta en marcha' },
            { archivo: 'docs/arquitectura.md', titulo: 'Arquitectura' },
            { archivo: 'docs/pantallas.md', titulo: 'Pantallas' },
            { archivo: 'docs/seguridad.md', titulo: 'Seguridad' },
            { archivo: 'docs/patrones.md', titulo: 'Patrones' },
            { archivo: 'docs/graficos-y-tema.md', titulo: 'Gráficos y tema' },
            { archivo: 'docs/despliegue.md', titulo: 'Despliegue' },
            { archivo: 'docs/decisiones.md', titulo: 'Registro de decisiones' },
        ],
    },
    psicologo: {
        tipo: 'usuario', raiz: CARPETA_USUARIO, archivo: 'manual-psicologo.md',
        subtitulo: 'Manual del Psicólogo', descripcion: 'Profesional que aplica y califica la batería',
    },
    administrador: {
        tipo: 'usuario', raiz: CARPETA_USUARIO, archivo: 'manual-administrador.md',
        subtitulo: 'Manual del Administrador', descripcion: 'Usuarios, empresas, evaluadores y configuración',
    },
    cliente: {
        tipo: 'usuario', raiz: CARPETA_USUARIO, archivo: 'manual-cliente.md',
        subtitulo: 'Manual del Cliente', descripcion: 'Usuario de la empresa evaluada',
    },
};

const NOMBRE_PROYECTO = argumento('--proyecto') ?? 'backend';
const PROYECTO = PROYECTOS[NOMBRE_PROYECTO];
if (!PROYECTO) {
    console.error(`Proyecto desconocido: ${NOMBRE_PROYECTO} (opciones: ${Object.keys(PROYECTOS).join(', ')})`);
    process.exit(1);
}
const RAIZ = path.resolve(argumento('--raiz') ?? PROYECTO.raiz);
const DE_USUARIO = PROYECTO.tipo === 'usuario';
const CAPITULOS = DE_USUARIO ? capitulosDeUnArchivo(path.join(RAIZ, PROYECTO.archivo)) : PROYECTO.capitulos;

const TITULO = DE_USUARIO ? 'PsicoBatería' : 'Manual técnico PsicoBatería';
const SUBTITULO = PROYECTO.subtitulo;
const SALIDA_POR_DEFECTO = DE_USUARIO
    ? path.join(CARPETA_USUARIO, `${SUBTITULO} – PsicoBatería.docx`)
    : path.join(CARPETA_SALIDA, `${TITULO} – ${SUBTITULO}.docx`);

/** Un .md con un capítulo por cada título `#` (lo de antes del primero se ignora). */
function capitulosDeUnArchivo(ruta) {
    const capitulos = [];
    for (const t of marked.lexer(fs.readFileSync(ruta, 'utf8'))) {
        if (t.type === 'heading' && t.depth === 1) capitulos.push({ archivo: path.basename(ruta), titulo: decodificar(t.text), tokens: [] });
        else if (capitulos.length) capitulos.at(-1).tokens.push(t);
    }
    return capitulos;
}

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

/**
 * Capítulo al que apunta una ruta como "docs/seguridad.md", "seguridad.md", "../README.md" o "README.md". Una ruta
 * de otro repositorio ("baterias_backend/docs/seguridad.md") no es un capítulo de este manual: queda como código.
 */
function capituloDe(ruta) {
    const limpia = ruta.split('#')[0].replace(/^(\.\.?\/)+/, '');
    return CAPITULOS.findIndex(c => c.archivo === limpia || path.basename(c.archivo) === limpia);
}

function decodificar(texto) {
    return texto.replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>').replace(/&amp;/g, '&');
}

/** Ancho y alto de un JPEG (marcador SOF). */
function tamanoJpeg(buffer) {
    let i = 2;
    while (i < buffer.length) {
        const marca = buffer[i + 1];
        const largo = buffer.readUInt16BE(i + 2);
        if (marca >= 0xC0 && marca <= 0xC3) return { ancho: buffer.readUInt16BE(i + 7), alto: buffer.readUInt16BE(i + 5) };
        i += 2 + largo;
    }
    throw new Error('JPEG sin dimensiones');
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
let figuras = 0;

/** Captura de pantalla con su pie "Figura N. texto" (manuales de usuario: `![texto](capturas/rol/archivo.jpg)`). */
function figura(imagen) {
    figuras++;
    const datos = fs.readFileSync(path.join(RAIZ, decodeURIComponent(imagen.href)));
    const esPng = imagen.href.toLowerCase().endsWith('.png');
    const { ancho, alto } = esPng ? tamanoPng(datos) : tamanoJpeg(datos);
    const w = 560; // ~5,8": deja margen a los lados y cabe más texto en la página
    const h = Math.round(alto * w / ancho);
    return [
        new Paragraph({
            alignment: AlignmentType.CENTER, keepNext: true, spacing: { before: 160, after: 60 },
            children: [new ImageRun({ type: esPng ? 'png' : 'jpg', data: datos, transformation: { width: w, height: h },
                altText: { name: `Figura ${figuras}`, title: `Figura ${figuras}`, description: decodificar(imagen.text) } })],
        }),
        new Paragraph({
            alignment: AlignmentType.CENTER, spacing: { after: 240 },
            children: [new TextRun({ text: `Figura ${figuras}. ${decodificar(imagen.text)}`, italics: true, size: 18, color: '6B7280' })],
        }),
    ];
}

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
            case 'paragraph': {
                const imagenes = t.tokens.filter(x => x.type === 'image');
                if (imagenes.length === 1 && t.tokens.every(x => x.type === 'image' || !x.raw.trim())) return figura(imagenes[0]);
                return [this.parrafo(inline(t.tokens), contexto, t.text.trim().endsWith(':') ? { keepNext: true } : {})];
            }
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
        let texto = decodificar(t.text).replace(/`/g, '');
        const nivel = { 2: HeadingLevel.HEADING_2, 3: HeadingLevel.HEADING_3 }[t.depth] ?? HeadingLevel.HEADING_4;
        const id = marcador(`cap ${this.cap} ${texto}`);
        let contenido = inline(t.tokens);
        // Manuales de usuario: las secciones se numeran con el capítulo (2.1, 2.2…), como en el índice.
        if (DE_USUARIO && t.depth === 2) {
            texto = `${this.cap}.${this.secciones.length + 1} ${texto}`;
            contenido = [new TextRun(`${this.cap}.${this.secciones.length + 1} `), ...contenido];
        }
        if (t.depth === 2) this.secciones.push({ texto, id });
        return new Paragraph({ heading: nivel, children: [new Bookmark({ id, children: contenido })] });
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
    let tokens = CAPITULOS[indice].tokens;
    if (!tokens) {
        tokens = marked.lexer(fs.readFileSync(path.join(RAIZ, archivo), 'utf8'));
        const primerH1 = tokens.findIndex(t => t.type === 'heading' && t.depth === 1);
        if (primerH1 >= 0) tokens.splice(primerH1, 1);
    }
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
        // Manuales de usuario: los capítulos van seguidos (solo el primero empieza después del índice).
        pageBreakBefore: !DE_USUARIO || numero === 1,
        children: [new Bookmark({ id: marcador(`cap ${numero} ${titulo}`), children: [new TextRun(`${numero}. ${titulo}`)] })],
    });
    return { titulo, numero, secciones: conv.secciones, parrafos: [encabezado, ...cuerpo] };
}

/** Portada de un manual de usuario, como los de Control Procesos: nombre, manual, para quién, versión y aviso. */
function portadaUsuario() {
    const hoy = new Date().toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
    const version = argumento('--version') ?? '1.0';
    const linea = (texto, opciones = {}) => new Paragraph({ alignment: AlignmentType.CENTER, ...opciones.p, children: [new TextRun({ text: texto, font: FUENTE, ...opciones.r })] });
    return [
        new Paragraph({ spacing: { before: 3400 }, children: [] }),
        linea('PsicoBatería', { r: { size: 64, color: AZUL, bold: true }, p: { spacing: { after: 60 } } }),
        linea(SUBTITULO, { r: { size: 36, color: '111827' }, p: { spacing: { after: 480 } } }),
        linea(PROYECTO.descripcion, { r: { size: 24, color: '4B5563' }, p: { spacing: { after: 2600 } } }),
        linea(`Versión ${version} · ${hoy}`, { r: { size: 22, color: '111827' } }),
        linea('Documento interno. No publicar.', { r: { size: 20, italics: true, color: 'B91C1C' } }),
    ];
}

function portada() {
    if (DE_USUARIO) return portadaUsuario();
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
        description: DE_USUARIO ? `${SUBTITULO} de PsicoBatería` : `Documentación técnica del ${SUBTITULO.toLowerCase()}, generada desde README.md y docs/*.md`,
        styles: {
            default: {
                document: { run: { font: FUENTE, size: 21, color: '1F2937' }, paragraph: { spacing: { after: 120, line: 276 } } },
                heading1: encabezadoTitulo(36, AZUL, DE_USUARIO ? 480 : 0, 240), // en los de usuario los capítulos van seguidos
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
                properties: { page: { size: { width: ANCHO_PAGINA, height: ALTO_PAGINA }, margin: { top: MARGEN, bottom: MARGEN, left: MARGEN, right: MARGEN }, ...(DE_USUARIO ? {} : { pageNumbers: { start: 1 } }) } },
                headers: { default: new Header({ children: [new Paragraph({
                    alignment: AlignmentType.RIGHT,
                    border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: GRIS_BORDE, space: 4 } },
                    children: [new TextRun({ text: DE_USUARIO ? `${TITULO} · ${SUBTITULO}` : `${TITULO} – ${SUBTITULO}`, size: 16, color: '6B7280' })],
                })] }) },
                footers: { default: new Footer({ children: [new Paragraph({
                    alignment: AlignmentType.CENTER,
                    children: [new TextRun({ children: DE_USUARIO ? ['Página ', PageNumber.CURRENT, ' de ', PageNumber.TOTAL_PAGES] : ['Página ', PageNumber.CURRENT], size: 16, color: '6B7280' })],
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
    console.log(`  ${capitulos.length} capítulos, ${diagramas} diagramas${DE_USUARIO ? `, ${figuras} figuras` : ''}`);
} finally {
    fs.rmSync(temporal, { recursive: true, force: true });
}
