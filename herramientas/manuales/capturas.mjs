// Capturas de pantalla para los manuales de usuario, con datos ficticios.
//
//   npm run capturas -- --rol psicologo [--salida <carpeta>] [--front <ruta de baterias_front>]
//
// Necesita el front (http://localhost:4201) y el backend (http://localhost:8080) corriendo. Entra con los usuarios de
// prueba de environment.development.ts del front, recorre las pantallas del rol y guarda una imagen por figura.
//
// Datos: todas las respuestas de la API pasan por `anonimizar` antes de llegar al navegador (personas, empresas,
// documentos, NIT, contactos y correos se cambian por datos inventados, siempre el mismo por cada valor original).
// Antes de cada captura se revisa que en la pantalla no quede ningún valor original: si queda, no se guarda.
// Solo se mira: el script no guarda ni envía nada (no genera enlaces: el backend local envía correos de verdad).

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import puppeteer from 'puppeteer';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const RAIZ_BACKEND = path.resolve(AQUI, '..', '..');

function argumento(nombre) {
    const i = process.argv.indexOf(nombre);
    return i >= 0 ? process.argv[i + 1] : undefined;
}

const ROL = argumento('--rol') ?? 'psicologo';
const FRONT = path.resolve(argumento('--front') ?? path.resolve(RAIZ_BACKEND, '..', '..', 'frontend', 'baterias_front'));
const SALIDA = path.resolve(argumento('--salida') ?? path.join(os.homedir(), 'proyectos', 'Documentacion', 'baterias',
    'Manuales de usuario', 'capturas', ROL));
const URL_FRONT = 'http://localhost:4201';
const URL_API = 'http://localhost:8080/api';
const ANIO = new Date().getFullYear();

// Ventana de 1200 × 750 a 1,5x: imágenes de 1800 × 1125, nítidas en Word a 6,5".
const VENTANA = { width: 1200, height: 750, deviceScaleFactor: 1.5 };

// ---------------------------------------------------------------------------------------------
// Datos ficticios
// ---------------------------------------------------------------------------------------------

const NOMBRES = ['Ana María', 'Carlos', 'Diana', 'Andrés', 'Laura', 'Jorge', 'Paola', 'Felipe', 'Natalia', 'Julián',
    'Sandra', 'Mauricio', 'Camila', 'Ricardo', 'Valentina', 'Óscar', 'Juliana', 'Hernán', 'Marcela', 'Sebastián'];
const APELLIDOS = ['Gómez Ríos', 'Rojas Peña', 'Martínez Cano', 'Herrera Vidal', 'Castro Mejía', 'Ortiz Lara',
    'Suárez Mora', 'Parra Díaz', 'Ramírez Sosa', 'Torres León', 'Vargas Gil', 'Moreno Ruiz', 'Cárdenas Paz',
    'Jiménez Rey', 'Salazar Luna', 'Acosta Vega', 'Navarro Cruz', 'Molina Rivas', 'Aguilar Soto', 'Benítez Ortega'];
const EMPRESAS = ['Empresa Demo S.A.S.', 'Industrias Ejemplo S.A.S.', 'Comercializadora Muestra Ltda.',
    'Servicios Modelo S.A.', 'Manufacturas Prueba S.A.S.', 'Logística Ejemplar S.A.S.'];

/** Valores originales que no pueden aparecer en ninguna captura, con su reemplazo. */
const reemplazos = new Map();
const contadores = { persona: 0, empresa: 0, correo: 0, evaluador: 0, usuario: 0, ip: 0 };

/** Ya inventado o de prueba: se deja igual. */
/** Nombres de rol y genéricos: no son datos personales aunque un usuario se llame así. */
const GENERICOS = new Set(['Administrador', 'Psicólogo', 'Psicóloga', 'Cliente', 'Evaluado']);

const esFicticio = valor => typeof valor !== 'string' || valor.length < 3 || GENERICOS.has(valor)
    || /prueba|@baterias\.local$/i.test(valor)
    || [...reemplazos.values()].includes(valor);

function reemplazo(original, crear) {
    if (original == null || esFicticio(original)) return original;
    if (!reemplazos.has(original)) reemplazos.set(original, crear());
    return reemplazos.get(original);
}

function anonimizarPersona(o) {
    const n = contadores.persona++;
    const documento = String(o.numeroIdentificacion);
    if (esFicticio(o.nombre) && !('apellido' in o)) {
        // Evaluador de prueba ("Psicóloga de Prueba"): solo se cambian sus números.
        o.numeroIdentificacion = reemplazo(documento, () => String(52000000 + n));
    } else {
        o.numeroIdentificacion = reemplazo(documento, () => String(1010200300 + n * 7919));
        if ('apellido' in o) {
            o.nombre = reemplazo(o.nombre, () => NOMBRES[n % NOMBRES.length]);
            o.apellido = reemplazo(o.apellido, () => APELLIDOS[n % APELLIDOS.length]);
        } else {
            o.nombre = reemplazo(o.nombre, () => `${NOMBRES[n % NOMBRES.length]} ${APELLIDOS[n % APELLIDOS.length]}`);
        }
    }
    if (o.tarjetaProfesional) o.tarjetaProfesional = reemplazo(o.tarjetaProfesional, () => `TP-${100200 + n}`);
    if (o.licenciaSaludOcupacional) o.licenciaSaludOcupacional = reemplazo(o.licenciaSaludOcupacional, () => `LSO-${3300 + n}`);
}

/** Las empresas se reemplazan siempre, aunque su nombre diga "prueba" (en pantalla parecería un error). */
function nombreEmpresa(original) {
    if (original == null || EMPRESAS.includes(original)) return original;
    if (!reemplazos.has(original)) reemplazos.set(original, EMPRESAS[contadores.empresa++ % EMPRESAS.length]);
    return reemplazos.get(original);
}

/** Ingresos hechos por scripts (curl, node): en la captura, navegadores de ejemplo. */
const NAVEGADORES = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36 Edg/131.0',
];
let navegadores = 0;
const navegadorEjemplo = original => /Mozilla/.test(original ?? '') ? original : NAVEGADORES[navegadores++ % NAVEGADORES.length];

function anonimizarEmpresa(o) {
    const n = contadores.empresa;
    o.nombre = nombreEmpresa(o.nombre);
    o.nit = reemplazo(String(o.nit), () => `90012345${n}`);
    if (o.nombreContacto) o.nombreContacto = reemplazo(o.nombreContacto, () => 'Contacto de Ejemplo');
    if (o.direccion) o.direccion = reemplazo(o.direccion, () => 'Calle 10 # 20-30');
    for (const campo of ['telefono', 'telefonoContacto']) {
        if (o[campo]) o[campo] = reemplazo(String(o[campo]), () => `60140000${n}${campo.length % 10}`);
    }
}

const nombreEvaluador = original => reemplazo(original, () =>
    `${NOMBRES[(contadores.evaluador + 7) % NOMBRES.length]} ${APELLIDOS[(contadores.evaluador++ + 7) % APELLIDOS.length]}`);
const correo = original => reemplazo(original, () => `usuario${++contadores.correo}@empresa-demo.co`);
/** Nombre de un usuario del sistema (p. ej. el de la empresa cliente). */
const nombreUsuario = original => reemplazo(original, () =>
    ['Talento Humano Demo', 'Coordinación SST Demo', 'Usuario de Ejemplo'][contadores.usuario++ % 3]);
/** IP de ejemplo (rango de documentación 203.0.113.0/24). */
const ip = original => reemplazo(original, () => `203.0.113.${10 + contadores.ip++ * 7}`);

/** Textos de ejemplo para las conclusiones del informe de empresa (en la BD local hay textos de prueba). */
const CONCLUSIONES_EJEMPLO = {
    conclusiones: 'El 40,7 % de los trabajadores evaluados presenta riesgo intralaboral alto o muy alto, concentrado en '
        + 'el dominio Recompensas y en las áreas de Producción y Operaciones. El nivel de síntomas de estrés es medio.',
    accionesCortoPlazo: 'Socializar los resultados con la gerencia y el COPASST. Programa de pausas activas.',
    accionesMedianoPlazo: 'Plan de reconocimiento y retroalimentación del desempeño. Talleres de liderazgo.',
    accionesLargoPlazo: 'Revisión de cargas de trabajo y nueva aplicación de la batería según la periodicidad.',
};

/** Recorre la respuesta: primero por la forma de cada objeto, luego cualquier texto que contenga un valor original. */
function anonimizar(dato) {
    const recorrer = o => {
        if (Array.isArray(o)) return o.forEach(recorrer);
        if (!o || typeof o !== 'object') return;
        if ('nit' in o && 'nombre' in o) anonimizarEmpresa(o);
        else if ('numeroIdentificacion' in o) anonimizarPersona(o);
        if ('rol' in o && typeof o.nombre === 'string' && !('numeroIdentificacion' in o)) o.nombre = nombreUsuario(o.nombre);
        if (typeof o.nombreUsuario === 'string') o.nombreUsuario = nombreUsuario(o.nombreUsuario);
        if (typeof o.ip === 'string') o.ip = ip(o.ip);
        if ('navegador' in o && 'resultado' in o) o.navegador = navegadorEjemplo(o.navegador);
        if (typeof o.cliente === 'string') o.cliente = nombreEmpresa(o.cliente);
        if (typeof o.evaluador === 'string') o.evaluador = nombreEvaluador(o.evaluador);
        if (typeof o.correo === 'string') o.correo = correo(o.correo);
        if (typeof o.nombreContacto === 'string') o.nombreContacto = reemplazo(o.nombreContacto, () => 'Contacto de Ejemplo');
        if ('accionesCortoPlazo' in o) {
            for (const [campo, texto] of Object.entries(CONCLUSIONES_EJEMPLO)) if (o[campo]) o[campo] = texto;
        }
        Object.values(o).forEach(recorrer);
    };
    recorrer(dato);
    return reemplazarEnTextos(dato);
}

/** Cualquier texto que contenga un valor original (p. ej. el nombre de la empresa en las conclusiones). */
function reemplazarEnTextos(o) {
    if (typeof o === 'string') {
        let texto = o;
        for (const [original, nuevo] of reemplazos) {
            if (original.length >= 4 && texto.includes(original)) texto = texto.split(original).join(nuevo);
        }
        return texto;
    }
    if (Array.isArray(o)) return o.map(reemplazarEnTextos);
    if (o && typeof o === 'object') {
        for (const k of Object.keys(o)) o[k] = reemplazarEnTextos(o[k]);
    }
    return o;
}

// ---------------------------------------------------------------------------------------------
// Sesión y navegador
// ---------------------------------------------------------------------------------------------

function credenciales(correoUsuario) {
    const texto = fs.readFileSync(path.join(FRONT, 'src', 'environments', 'environment.development.ts'), 'utf8');
    const m = new RegExp(`correo: '${correoUsuario.replace(/[.+]/g, '\\$&')}', contrasena: '([^']*)'`).exec(texto);
    if (!m) throw new Error(`No está ${correoUsuario} en environment.development.ts del front`);
    return m[1];
}

/** Correo del usuario de prueba cuya etiqueta empieza por el texto ("Cliente", "Administrador"). */
function correoDe(etiqueta) {
    const texto = fs.readFileSync(path.join(FRONT, 'src', 'environments', 'environment.development.ts'), 'utf8');
    const m = new RegExp(`etiqueta: '${etiqueta}[^']*', correo: '([^']*)'`).exec(texto);
    if (!m) throw new Error(`No hay usuario de prueba "${etiqueta}" en environment.development.ts del front`);
    return m[1];
}

async function api(ruta, token, opciones = {}) {
    const r = await fetch(`${URL_API}${ruta}`, {
        ...opciones,
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });
    if (!r.ok) throw new Error(`${ruta}: ${r.status}`);
    return (await r.json()).data;
}

async function iniciarSesion(correoUsuario) {
    const sesion = await api('/auth/login', null,
        { method: 'POST', body: JSON.stringify({ correo: correoUsuario, contrasena: credenciales(correoUsuario) }) });
    return sesion;
}

/**
 * Firma de ejemplo: las firmas son imágenes (el chequeo de texto no las ve), así que cualquier firma que llegue del
 * backend se reemplaza por este trazo.
 */
const FIRMA_EJEMPLO = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="160" viewBox="0 0 400 160">
  <rect width="400" height="160" fill="white"/>
  <path d="M30 110 C 60 40, 90 40, 110 95 S 160 140, 185 80 S 230 30, 255 90 S 310 130, 340 70 L 370 60"
        fill="none" stroke="#1f2937" stroke-width="4" stroke-linecap="round"/>
  <line x1="40" y1="128" x2="360" y2="128" stroke="#1f2937" stroke-width="2"/>
</svg>`;

/** Oculta las ayudas de desarrollo (selector de usuario, "Entrar como", sección Desarrollo del Inicio). */
const CSS_SIN_DESARROLLO = `
  app-topbar p-tag, app-topbar .selector-usuario, .desarrollo { display: none !important; }
  * { caret-color: transparent !important; }
`;

async function nuevaPagina(navegador, { sesion = null, bloquearLogin = false } = {}) {
    const pagina = await navegador.newPage();
    await pagina.setViewport(VENTANA);
    if (sesion) {
        const usuario = anonimizar(structuredClone(sesion.usuario));
        await pagina.evaluateOnNewDocument((token, expira, usuario) => {
            if (location.origin === 'http://localhost:4201' && !sessionStorage.getItem('sesion-puesta')) {
                localStorage.setItem('token', token);
                localStorage.setItem('token-expira', String(expira));
                localStorage.setItem('usuario', usuario);
                localStorage.setItem('app-theme', 'light');
                sessionStorage.setItem('sesion-puesta', '1');
            }
        }, sesion.token, new Date(sesion.expira).getTime(), JSON.stringify(usuario));
    }
    const cdp = await pagina.createCDPSession();
    await cdp.send('Fetch.enable', {
        patterns: [
            { urlPattern: `${URL_API}/*`, requestStage: 'Request' },
            { urlPattern: `${URL_API}/*`, requestStage: 'Response' },
        ],
    });
    cdp.on('Fetch.requestPaused', async evento => {
        const { requestId, request, responseStatusCode, responseHeaders } = evento;
        try {
            if (responseStatusCode === undefined) {
                // Etapa de la petición: en la captura del login, el auto-login de desarrollo recibe un 401 (sin aviso).
                if (bloquearLogin && request.url.endsWith('/auth/login')) {
                    const cuerpo = JSON.stringify({ success: false, message: 'Usuario o contraseña incorrectos', errorCode: 'UNAUTHORIZED' });
                    return await cdp.send('Fetch.fulfillRequest', {
                        requestId, responseCode: 401, body: Buffer.from(cuerpo).toString('base64'),
                        responseHeaders: [{ name: 'Content-Type', value: 'application/json' },
                            { name: 'Access-Control-Allow-Origin', value: URL_FRONT }],
                    });
                }
                if (request.method !== 'GET' && request.method !== 'OPTIONS' && !request.url.endsWith('/auth/login')) {
                    throw new Error(`El script no debe guardar nada: ${request.method} ${request.url}`);
                }
                return await cdp.send('Fetch.continueRequest', { requestId });
            }
            const tipo = (responseHeaders ?? []).find(h => h.name.toLowerCase() === 'content-type')?.value ?? '';
            if (/\/firma(\?|$)/.test(request.url) && tipo.startsWith('image/')) {
                return await cdp.send('Fetch.fulfillRequest', {
                    requestId, responseCode: 200, body: Buffer.from(FIRMA_EJEMPLO).toString('base64'),
                    responseHeaders: [...responseHeaders.filter(h => !/^content-(type|length)$/i.test(h.name)),
                        { name: 'Content-Type', value: 'image/svg+xml' }],
                });
            }
            if (!tipo.includes('json')) return await cdp.send('Fetch.continueResponse', { requestId });
            const { body, base64Encoded } = await cdp.send('Fetch.getResponseBody', { requestId });
            const json = JSON.parse(base64Encoded ? Buffer.from(body, 'base64').toString('utf8') : body);
            const nuevo = Buffer.from(JSON.stringify(anonimizar(json)), 'utf8');
            await cdp.send('Fetch.fulfillRequest', {
                requestId, responseCode: responseStatusCode,
                responseHeaders: responseHeaders.filter(h => h.name.toLowerCase() !== 'content-length'),
                body: nuevo.toString('base64'),
            });
        } catch (e) {
            console.error(`  ! ${request.method} ${request.url}: ${e.message}`);
            try { await cdp.send('Fetch.failRequest', { requestId, errorReason: 'Failed' }); } catch { /* ya resuelta */ }
        }
    });
    await pagina.evaluateOnNewDocument(css => {
        document.addEventListener('DOMContentLoaded', () => {
            const estilo = document.createElement('style');
            estilo.textContent = css;
            document.head.appendChild(estilo);
        });
    }, CSS_SIN_DESARROLLO);
    return pagina;
}

const esperar = ms => new Promise(r => setTimeout(r, ms));

async function ir(pagina, ruta) {
    await pagina.goto(`${URL_FRONT}${ruta}`, { waitUntil: 'networkidle0' });
    await esperar(600);
}

async function clic(pagina, selector) {
    await pagina.waitForSelector(selector, { visible: true, timeout: 8000 });
    await pagina.click(selector);
    await esperar(700);
}

/** Clic en el botón (o enlace) que contiene el texto. */
async function clicTexto(pagina, texto, etiqueta = 'button') {
    await clic(pagina, `${etiqueta} ::-p-text(${texto})`);
}

/** Clic en el ⚙️ de la fila de la tabla que contiene el texto. */
async function clicFila(pagina, texto) {
    const ok = await pagina.evaluate(t => {
        const fila = [...document.querySelectorAll('p-table tr')].find(tr => tr.innerText.includes(t));
        const boton = fila?.querySelector('p-button[icon="pi pi-cog"] button');
        boton?.click();
        return !!boton;
    }, texto);
    if (!ok) throw new Error(`No hay una fila con "${texto}"`);
    await esperar(700);
}

async function ocultarDesarrolloDelInicio(pagina) {
    await pagina.evaluate(() => {
        const titulo = [...document.querySelectorAll('h2')].find(h => h.textContent.trim() === 'Desarrollo');
        if (!titulo) return;
        let nodo = titulo;
        while (nodo) {
            const siguiente = nodo.nextElementSibling;
            nodo.style.display = 'none';
            nodo = siguiente;
        }
    });
}

let figuras = 0;

/** Guarda la captura si en la pantalla no queda ningún dato original. */
async function capturar(pagina, nombre, descripcion) {
    await esperar(400);
    const texto = await pagina.evaluate(() => document.body.innerText + ' ' +
        [...document.querySelectorAll('input, textarea')].map(e => e.value).join(' '));
    const filtrados = [...reemplazos.keys()].filter(v => v.length >= 4 && texto.includes(v));
    if (filtrados.length) {
        console.error(`  ✗ ${nombre}: quedan ${filtrados.length} dato(s) original(es) en pantalla; no se guarda`);
        return;
    }
    fs.mkdirSync(SALIDA, { recursive: true });
    await pagina.screenshot({ path: path.join(SALIDA, `${nombre}.jpg`), type: 'jpeg', quality: 85 });
    figuras++;
    console.log(`  ✓ ${nombre}.jpg  ${descripcion}`);
}

async function cerrarDialogo(pagina) {
    await pagina.keyboard.press('Escape');
    await esperar(500);
}

// ---------------------------------------------------------------------------------------------
// Recorridos por rol
// ---------------------------------------------------------------------------------------------

async function login(navegador) {
    const pagina = await nuevaPagina(navegador, { bloquearLogin: true });
    await ir(pagina, '/login');
    await capturar(pagina, '01-login', 'Inicio de sesión');
    await pagina.close();
}

async function psicologo(navegador) {
    const sesion = await iniciarSesion('psicologa@baterias.local');
    // Ejemplos tomados del listado (sin ids fijos): cliente con aplicaciones, una calculada de forma A con firmante y una pendiente.
    const listado = await api(`/aplicaciones/listado?anio=${ANIO}`, sesion.token);
    // La empresa con más aplicaciones calculadas: el informe y las estadísticas se ven completos.
    const calculadasPorCliente = new Map();
    listado.filter(a => a.estado === 'CALCULADA')
        .forEach(a => calculadasPorCliente.set(a.idCliente, (calculadasPorCliente.get(a.idCliente) ?? 0) + 1));
    const idCliente = [...calculadasPorCliente.entries()].sort((a, b) => b[1] - a[1])[0][0];
    const delCliente = listado.filter(a => a.idCliente === idCliente);
    const calculada = delCliente.find(a => a.estado === 'CALCULADA' && a.forma === 'A' && a.idEvaluador)
        ?? delCliente.find(a => a.estado === 'CALCULADA');
    // Para la digitación, una con respuestas a medias; si no hay, una calculada (todas respondidas).
    const pendiente = delCliente.find(a => a.estado === 'INCOMPLETA') ?? delCliente.find(a => a.estado === 'PENDIENTE_CALCULO')
        ?? calculada;
    const p = await nuevaPagina(navegador, { sesion });

    await ir(p, '/inicio');
    await ocultarDesarrolloDelInicio(p);
    await capturar(p, '02-inicio', 'Inicio del psicólogo');

    await clic(p, 'app-topbar button.usuario');
    await capturar(p, '03-menu-usuario', 'Menú del nombre');
    await cerrarDialogo(p);

    await ir(p, '/clientes');
    await capturar(p, '04-clientes', 'Clientes');
    await clic(p, 'p-table p-button[icon="pi pi-cog"] button');
    await capturar(p, '05-cliente-acciones', 'Acciones del cliente');
    await cerrarDialogo(p);

    await ir(p, `/clientes/${idCliente}/personas`);
    await capturar(p, '06-personas', 'Personas');
    await clicTexto(p, 'Vincular persona');
    await capturar(p, '07-vincular-persona', 'Vincular persona');
    await cerrarDialogo(p);
    await clicTexto(p, 'Cargar archivo');
    await capturar(p, '08-carga-archivo', 'Cargar personas desde Excel');
    await cerrarDialogo(p);

    // Acciones de la persona de la aplicación calculada: se busca por su documento (ya ficticio en pantalla).
    const buscar = async idEvaluado => {
        const personas = anonimizar(await api(`/clientes/${idCliente}/personas`, sesion.token));
        const persona = personas.find(x => x.idEvaluado === idEvaluado);
        await p.click('input[placeholder]', { count: 3 });
        await p.type('input[placeholder]', persona.numeroIdentificacion);
        await esperar(700);
    };
    await buscar(calculada.idEvaluado);
    await clic(p, 'p-table p-button[icon="pi pi-cog"] button');
    await capturar(p, '09-persona-acciones', 'Acciones de la persona');
    await clicTexto(p, 'Aplicaciones', 'button.action-card');
    await capturar(p, '10-persona-aplicaciones', 'Aplicaciones de la persona');
    await clicTexto(p, 'Nueva aplicación');
    await capturar(p, '11-nueva-aplicacion', 'Nueva aplicación');
    await cerrarDialogo(p);

    await ir(p, `/clientes/${idCliente}/personas`);
    await buscar(calculada.idEvaluado);
    await clic(p, 'p-table p-button[icon="pi pi-cog"] button');
    await clicTexto(p, 'Informe individual', 'button.action-card');
    await esperar(800);
    await capturar(p, '12-informe-individual', 'Informe individual');
    await cerrarDialogo(p);

    if (pendiente.id !== calculada.id) {
        await ir(p, `/aplicaciones/${pendiente.id}`);
        await capturar(p, '13-digitar', 'Digitar una aplicación');
    }
    await ir(p, `/aplicaciones/${calculada.id}`);
    await capturar(p, '14-resultados', 'Aplicación calculada: respuestas y resultados');

    await ir(p, `/aplicaciones?anio=${ANIO}`);
    await capturar(p, '15-aplicaciones', 'Listado de aplicaciones');

    await ir(p, `/clientes/${idCliente}/informe?anio=${ANIO}`);
    await capturar(p, '16-informe-empresa', 'Informe de empresa');
    await p.evaluate(() => document.querySelector('app-informe-conclusiones')?.scrollIntoView({ block: 'start' }));
    await capturar(p, '17-informe-conclusiones', 'Conclusiones y plan de acción');

    await ir(p, `/clientes/${idCliente}/estadisticas?anio=${ANIO}`);
    await capturar(p, '18-estadisticas-poblacion', 'Estadísticas: Población');
    for (const [pestana, nombre] of [['Intralaboral', '19-estadisticas-intralaboral'],
        ['Extralaboral', '20-estadisticas-extralaboral'], ['Estrés', '21-estadisticas-estres']]) {
        await clic(p, `p-tab ::-p-text(${pestana})`);
        await esperar(900);
        await capturar(p, nombre, `Estadísticas: ${pestana}`);
    }

    await ir(p, '/mi-firma');
    await capturar(p, '22-mi-firma', 'Mi firma');

    await ir(p, '/inicio');
    await clic(p, 'app-topbar button.usuario');
    await clicTexto(p, 'Cambiar mi contraseña', '.p-menu-item-link');
    await capturar(p, '23-cambiar-contrasena', 'Cambiar mi contraseña');
    await cerrarDialogo(p);

    await ir(p, '/configuracion/cuestionarios');
    await capturar(p, '24-cuestionarios', 'Configuración: Cuestionarios');
    await p.close();
}

async function cliente(navegador) {
    const sesion = await iniciarSesion(correoDe('Cliente'));
    const idCliente = sesion.usuario.idCliente;
    const p = await nuevaPagina(navegador, { sesion });

    await ir(p, '/inicio');
    await ocultarDesarrolloDelInicio(p);
    await capturar(p, '02-inicio', 'Inicio del cliente');
    await clic(p, 'app-topbar button.usuario');
    await capturar(p, '03-menu-usuario', 'Menú del nombre');
    await cerrarDialogo(p);

    await ir(p, `/clientes/${idCliente}/informe?anio=${ANIO}`);
    await capturar(p, '04-informe-empresa', 'Informe de empresa');
    await p.evaluate(() => document.querySelector('app-informe-conclusiones')?.scrollIntoView({ block: 'start' }));
    await capturar(p, '05-informe-conclusiones', 'Conclusiones (solo lectura)');

    await ir(p, `/clientes/${idCliente}/estadisticas?anio=${ANIO}`);
    await capturar(p, '06-estadisticas-poblacion', 'Estadísticas: Población');
    for (const [pestana, nombre] of [['Intralaboral', '07-estadisticas-intralaboral'],
        ['Extralaboral', '08-estadisticas-extralaboral'], ['Estrés', '09-estadisticas-estres']]) {
        await clic(p, `p-tab ::-p-text(${pestana})`);
        await esperar(900);
        await capturar(p, nombre, `Estadísticas: ${pestana}`);
    }

    await clic(p, 'app-topbar button.usuario');
    await clicTexto(p, 'Cambiar mi contraseña', '.p-menu-item-link');
    await capturar(p, '10-cambiar-contrasena', 'Cambiar mi contraseña');
    await p.close();
}

async function administrador(navegador) {
    const sesion = await iniciarSesion(correoDe('Administrador'));
    const p = await nuevaPagina(navegador, { sesion });

    await ir(p, '/inicio');
    await ocultarDesarrolloDelInicio(p);
    await capturar(p, '02-inicio', 'Inicio del administrador');
    await clic(p, 'app-topbar button.usuario');
    await capturar(p, '03-menu-usuario', 'Menú del nombre');
    await cerrarDialogo(p);

    await ir(p, '/clientes');
    await capturar(p, '04-clientes', 'Clientes');
    await clicTexto(p, 'Crear cliente');
    await capturar(p, '05-crear-cliente', 'Crear cliente');
    await cerrarDialogo(p);
    await clic(p, 'p-table p-button[icon="pi pi-cog"] button');
    await capturar(p, '06-cliente-acciones', 'Acciones del cliente');
    await clicTexto(p, 'Editar datos', 'button.action-card');
    await capturar(p, '07-editar-cliente', 'Editar cliente');
    await cerrarDialogo(p);

    await ir(p, '/administracion/usuarios');
    await capturar(p, '08-usuarios', 'Usuarios');
    await clicTexto(p, 'Nuevo usuario');
    await capturar(p, '09-nuevo-usuario', 'Nuevo usuario');
    await cerrarDialogo(p);
    await clicFila(p, 'psicologa@baterias.local');
    await capturar(p, '10-usuario-acciones', 'Acciones del usuario');
    await clicTexto(p, 'Clientes asignados', 'button.action-card');
    await capturar(p, '11-clientes-asignados', 'Clientes asignados');
    await cerrarDialogo(p);
    await clicFila(p, 'psicologa@baterias.local');
    await clicTexto(p, 'Enlace de contraseña', 'button.action-card');
    await capturar(p, '12-enlace-contrasena', 'Enlace de contraseña (sin confirmar)');
    await cerrarDialogo(p);

    await ir(p, '/administracion/evaluadores');
    await capturar(p, '13-evaluadores', 'Evaluadores');
    await clicTexto(p, 'Nuevo evaluador');
    await capturar(p, '14-nuevo-evaluador', 'Nuevo evaluador');
    await cerrarDialogo(p);
    await clic(p, 'p-table p-button[icon="pi pi-cog"] button');
    await capturar(p, '15-evaluador-acciones', 'Acciones del evaluador');
    await clicTexto(p, 'Firma', 'button.action-card');
    await capturar(p, '16-evaluador-firma', 'Firma del evaluador');
    await cerrarDialogo(p);

    await ir(p, '/administracion/ingresos');
    await capturar(p, '17-ingresos', 'Ingresos');

    await ir(p, '/configuracion/catalogos');
    await capturar(p, '18-catalogos', 'Configuración: Catálogos');
    await ir(p, '/configuracion/cuestionarios');
    await capturar(p, '19-cuestionarios', 'Configuración: Cuestionarios');
    await p.close();
}

const RECORRIDOS = { psicologo, cliente, administrador };

// ---------------------------------------------------------------------------------------------

if (!RECORRIDOS[ROL]) {
    console.error(`Rol sin recorrido: ${ROL} (opciones: ${Object.keys(RECORRIDOS).join(', ')})`);
    process.exit(1);
}
const navegador = await puppeteer.launch({ headless: true });
try {
    console.log(`Capturas de ${ROL} → ${SALIDA}`);
    await login(navegador);
    await RECORRIDOS[ROL](navegador);
    console.log(`${figuras} capturas`);
} finally {
    await navegador.close();
}
