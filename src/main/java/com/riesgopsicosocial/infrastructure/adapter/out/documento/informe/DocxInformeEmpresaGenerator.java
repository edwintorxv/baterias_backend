package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.empresa.AnalisisGrupo;
import com.riesgopsicosocial.domain.model.informe.empresa.Cobertura;
import com.riesgopsicosocial.domain.model.informe.empresa.CoberturaCiudad;
import com.riesgopsicosocial.domain.model.informe.empresa.ConteoFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.ConcentracionRiesgo;
import com.riesgopsicosocial.domain.model.informe.empresa.ConteoNivel;
import com.riesgopsicosocial.domain.model.informe.empresa.DesgloseGrupos;
import com.riesgopsicosocial.domain.model.informe.empresa.DistribucionDimension;
import com.riesgopsicosocial.domain.model.informe.empresa.DistribucionDominio;
import com.riesgopsicosocial.domain.model.informe.empresa.DistribucionGrupo;
import com.riesgopsicosocial.domain.model.informe.empresa.DistribucionNiveles;
import com.riesgopsicosocial.domain.model.informe.empresa.DominioConsolidado;
import com.riesgopsicosocial.domain.model.informe.empresa.DominiosForma;
import com.riesgopsicosocial.domain.model.informe.empresa.FrecuenciaDimension;
import com.riesgopsicosocial.domain.model.informe.empresa.InformeEmpresa;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.SintomasEstres;
import com.riesgopsicosocial.domain.model.resultado.NivelEstres;
import com.riesgopsicosocial.domain.service.informe.AnalizadorInformeEmpresa;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.DocxSoporte.*;

/**
 * Informe de empresa en Word: población evaluada, resultados globales (intralaboral,
 * extralaboral y estrés), por cada forma intralaboral ranking de dominios y de dimensiones
 * y una ficha por dimensión; luego la concentración del riesgo intralaboral con A y B juntas
 * (dominios, ciudad y área), los síntomas de estrés (frecuencia por dimensión, ciudad y área) y,
 * para llenar en Word, las conclusiones del profesional, el plan de acción y su firma. Solo trae resultados agregados, nunca datos individuales.
 */
@Component
public class DocxInformeEmpresaGenerator implements GeneradorDocumentoInformeEmpresaPort {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int[] ANCHOS_DATOS = {3876, 6324};
    private static final int[] ANCHOS_COBERTURA = {5000, 2400, 2800};
    private static final int[] ANCHOS_NIVELES = {5600, 2000, 2600};
    /** Ranking de dominios: dominio, personas y los cinco niveles. */
    private static final int[] ANCHOS_DOMINIOS = {3000, 950, 1250, 1250, 1250, 1250, 1250};
    /** Resumen de dimensiones: dimensión, dominio, personas y los cinco niveles. */
    private static final int[] ANCHOS_RESUMEN = {2700, 2000, 900, 920, 920, 920, 920, 920};
    /** Ficha de una dimensión: los cinco niveles. */
    private static final int[] ANCHOS_FICHA = {2040, 2040, 2040, 2040, 2040};
    /** Alto mínimo de los recuadros para escribir, en twips: unos 9 cm las conclusiones y 4 cm cada plazo. */
    private static final int ALTO_CONCLUSIONES = 5100;
    private static final int ALTO_ACCIONES = 2270;
    /** Ciudades o áreas por gráfico: con más, las barras quedan apiñadas. */
    private static final int GRUPOS_POR_GRAFICO = 2;
    private static final String CRITERIO_RANKING = "ordenados del más crítico al menos crítico: primero el de "
            + "mayor porcentaje de personas en riesgo muy alto; si hay empate, el de mayor porcentaje en riesgo "
            + "alto, y así sucesivamente.";

    @Override
    public FormatoDocumento formato() {
        return FormatoDocumento.DOCX;
    }

    @Override
    public byte[] generar(InformeEmpresa informe, LocalDate fechaElaboracion) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            configurarPagina(doc, "CONFIDENCIAL – Resultados agregados de la empresa; no contiene información individual");
            AnalisisGrupo general = informe.general();

            texto(parrafo(doc, ParagraphAlignment.CENTER, 0, 60),
                    "INFORME DE RESULTADOS DE LA EVALUACIÓN DE FACTORES DE RIESGO PSICOSOCIAL", 12.5, true, null);
            texto(parrafo(doc, ParagraphAlignment.CENTER, 0, 200),
                    "Informe de empresa – Año " + informe.anio(), 10, false, "444444");

            seccion(doc, "DATOS DE LA EMPRESA");
            tablaDatos(doc, new String[][]{
                    {"Nombre de la empresa", informe.cliente().nombre()},
                    {"NIT", informe.cliente().nit()},
                    {"Año evaluado", String.valueOf(informe.anio())},
                    {"Fecha de elaboración del informe", fechaElaboracion.format(FECHA)}});

            seccion(doc, "1. POBLACIÓN EVALUADA");
            tablaCobertura(doc, general);
            if (general.pendientesCalculo() > 0) {
                nota(doc, general.pendientesCalculo() == 1
                        ? "1 persona respondió las pruebas pero sus resultados aún no se han calculado; no se incluye en las tablas de resultados."
                        : general.pendientesCalculo() + " personas respondieron las pruebas pero sus resultados aún no se han calculado; no se incluyen en las tablas de resultados.");
            }

            seccion(doc, "2. RESULTADOS GLOBALES");
            subtitulo(doc, "Factores de riesgo psicosocial intralaborales");
            tablaNiveles(doc, "Nivel de riesgo", general.intralaboral(), c -> c.nivel().nombre());
            subtitulo(doc, "Factores de riesgo psicosocial extralaborales");
            tablaNiveles(doc, "Nivel de riesgo", general.extralaboral(), c -> c.nivel().nombre());
            subtitulo(doc, "Valoración del estrés");
            tablaNiveles(doc, "Nivel de síntomas de estrés", general.estres(), c -> NivelEstres.etiqueta(c.nivel().id()));

            int numero = 3;
            // Una sección por forma intralaboral, con todo lo suyo junto: dominios, dimensiones y fichas.
            for (DominiosForma forma : general.dominios()) {
                saltoDePagina(doc);
                seccion(doc, numero++ + ". RESULTADOS INTRALABORALES – FORMA " + forma.forma()
                        + " (" + personas(forma.total()).toUpperCase() + ")");
                rankingDominios(doc, forma);
                resumenDimensiones(doc, forma);
                for (DistribucionDominio dominio : forma.dominios()) {
                    fichasDimensiones(doc, forma.forma(), dominio);
                }
            }

            // Cada sección nueva va al final: primero el intralaboral con A y B juntas, luego el estrés.
            ConcentracionRiesgo concentracion = informe.concentracion();
            if (!concentracion.dominios().isEmpty()) {
                saltoDePagina(doc);
                seccion(doc, numero++ + ". CONCENTRACIÓN DEL RIESGO INTRALABORAL (FORMAS A Y B)");
                concentracionIntralaboral(doc, concentracion);
            }
            if (general.estres().total() > 0) {
                saltoDePagina(doc);
                seccion(doc, numero++ + ". SÍNTOMAS DE ESTRÉS");
                sintomasEstres(doc, informe.sintomas(), concentracion);
            }

            // Para llenar en Word: el sistema no guarda estos textos.
            saltoDePagina(doc);
            seccion(doc, numero++ + ". CONCLUSIONES DEL PROFESIONAL");
            espacioParaEscribir(doc, "Escriba aquí las conclusiones del profesional.", ALTO_CONCLUSIONES);
            seccion(doc, numero++ + ". PLAN DE ACCIÓN");
            for (String plazo : List.of("corto", "mediano", "largo")) {
                subtitulo(doc, "Acciones a " + plazo + " plazo");
                espacioParaEscribir(doc, "Escriba aquí las acciones a " + plazo + " plazo.", ALTO_ACCIONES);
            }
            firmaProfesional(doc);

            doc.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Word del informe de empresa", e);
        }
    }

    private void tablaDatos(XWPFDocument doc, String[][] filas) {
        XWPFTable tabla = tabla(doc, filas.length, ANCHOS_DATOS);
        for (int i = 0; i < filas.length; i++) {
            XWPFTableRow fila = tabla.getRow(i);
            celda(fila.getCell(0), filas[i][0], true, ParagraphAlignment.LEFT, GRIS_ETIQUETA, ANCHOS_DATOS[0]);
            celda(fila.getCell(1), filas[i][1], false, ParagraphAlignment.LEFT, null, ANCHOS_DATOS[1]);
        }
        espacio(doc);
    }

    /** Una fila por ciudad de trabajo y el total de la empresa. */
    private void tablaCobertura(XWPFDocument doc, AnalisisGrupo general) {
        XWPFTable tabla = tabla(doc, 1, ANCHOS_COBERTURA);
        encabezado(tabla.getRow(0), ANCHOS_COBERTURA, "Ciudad de trabajo", "Personas esperadas",
                "Cobertura (personas evaluadas)");
        for (CoberturaCiudad ciudad : general.coberturaPorCiudad()) {
            filaCobertura(tabla.createRow(), ciudad.ciudad(), ciudad.cobertura(), false, null);
        }
        filaCobertura(tabla.createRow(), "TOTAL", general.cobertura(), true, GRIS_TOTAL_GENERAL);
        espacio(doc);
    }

    private void filaCobertura(XWPFTableRow fila, String nombre, Cobertura cobertura, boolean negrilla, String color) {
        celda(fila.getCell(0), nombre, negrilla, ParagraphAlignment.LEFT, color, ANCHOS_COBERTURA[0]);
        celda(fila.getCell(1), String.valueOf(cobertura.esperados()), negrilla, ParagraphAlignment.CENTER, color,
                ANCHOS_COBERTURA[1]);
        celda(fila.getCell(2), cobertura.evaluados() + " (" + porcentaje(cobertura.porcentaje()) + ")", negrilla,
                ParagraphAlignment.CENTER, color, ANCHOS_COBERTURA[2]);
    }

    /** Cantidad y porcentaje de personas por nivel, cada nivel con su color, y fila de total. */
    private void tablaNiveles(XWPFDocument doc, String tituloNivel, DistribucionNiveles distribucion,
                              Function<ConteoNivel, String> etiqueta) {
        XWPFTable tabla = tabla(doc, 1, ANCHOS_NIVELES);
        encabezado(tabla.getRow(0), ANCHOS_NIVELES, tituloNivel, "Cantidad de personas", "Porcentaje");
        for (ConteoNivel conteo : distribucion.niveles()) {
            XWPFTableRow fila = tabla.createRow();
            String color = COLOR_NIVEL.get(conteo.nivel().id());
            celda(fila.getCell(0), etiqueta.apply(conteo), false, ParagraphAlignment.LEFT, color, ANCHOS_NIVELES[0]);
            celda(fila.getCell(1), String.valueOf(conteo.cantidad()), false, ParagraphAlignment.CENTER, null, ANCHOS_NIVELES[1]);
            celda(fila.getCell(2), porcentaje(conteo.porcentaje()), false, ParagraphAlignment.CENTER, null, ANCHOS_NIVELES[2]);
        }
        XWPFTableRow total = tabla.createRow();
        celda(total.getCell(0), "TOTAL", true, ParagraphAlignment.LEFT, GRIS_TOTAL_GENERAL, ANCHOS_NIVELES[0]);
        celda(total.getCell(1), String.valueOf(distribucion.total()), true, ParagraphAlignment.CENTER, GRIS_TOTAL_GENERAL, ANCHOS_NIVELES[1]);
        celda(total.getCell(2), distribucion.total() == 0 ? "—" : "100 %", true, ParagraphAlignment.CENTER,
                GRIS_TOTAL_GENERAL, ANCHOS_NIVELES[2]);
        espacio(doc);
        graficoNiveles(doc, distribucion, etiqueta);
    }

    /** Porcentaje de personas por nivel, cada barra con el color de su nivel, igual que en la tabla. */
    private void graficoNiveles(XWPFDocument doc, DistribucionNiveles distribucion,
                                Function<ConteoNivel, String> etiqueta) {
        if (distribucion.total() == 0) {
            return;
        }
        String[] niveles = distribucion.niveles().stream().map(etiqueta).toArray(String[]::new);
        Double[] porcentajes = distribucion.niveles().stream()
                .map(c -> c.porcentaje().doubleValue()).toArray(Double[]::new);
        List<String> colores = distribucion.niveles().stream().map(c -> COLOR_NIVEL.get(c.nivel().id())).toList();
        DocxGraficos.barras(doc, niveles, porcentajes, colores);
    }

    /** Recuadro de una celda con un texto guía en gris, para escribir en Word; crece con lo que se escriba. */
    private static void espacioParaEscribir(XWPFDocument doc, String guia, int alto) {
        XWPFTable recuadro = tabla(doc, 1, new int[]{ANCHO_TEXTO});
        XWPFTableRow fila = recuadro.getRow(0);
        fila.setHeight(alto); // alto mínimo: Word lo agranda si el texto no cabe
        XWPFTableCell celda = fila.getCell(0);
        celda(celda, null, false, ParagraphAlignment.LEFT, null, ANCHO_TEXTO);
        celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
        texto(celda.getParagraphs().get(0), guia, 9, false, "808080").setItalic(true);
        espacio(doc);
    }

    /** Datos del profesional en blanco y línea de firma, para llenar a mano. */
    private void firmaProfesional(XWPFDocument doc) {
        subtitulo(doc, "Profesional responsable");
        tablaDatos(doc, new String[][]{
                {"Nombre del profesional", ""},
                {"Profesión", ""},
                {"No. tarjeta profesional", ""},
                {"No. licencia en salud ocupacional", ""},
                {"Fecha de expedición de la licencia en salud ocupacional", ""}});
        XWPFParagraph linea = parrafo(doc, ParagraphAlignment.LEFT, 1200, 0);
        linea.setBorderTop(Borders.SINGLE);
        linea.setIndentationRight(5500);
        texto(linea, "Firma del profesional", 9.5, false, null);
    }

    /** Dominios con A y B juntas (ranking) y total intralaboral por ciudad de trabajo y por área. */
    private void concentracionIntralaboral(XWPFDocument doc, ConcentracionRiesgo concentracion) {
        nota(doc, "Las formas A y B se analizan juntas: cada persona cuenta con el nivel que le dio el baremo de "
                + "su propia forma.");

        subtitulo(doc, "Dominios intralaborales");
        nota(doc, "Dominios " + CRITERIO_RANKING).setKeepNext(true);
        unirConElGrafico(doc, tablaPorNivel(doc, ANCHOS_DOMINIOS, new String[]{"Dominio", "Personas"},
                concentracion.dominios().stream()
                        .map(d -> new FilaPorNivel(new String[]{d.nombre(), String.valueOf(d.distribucion().total())},
                                d.distribucion()))
                        .toList(), NivelRiesgo::nombre));
        graficoPorNivel(doc, concentracion.dominios().stream().map(DominioConsolidado::nombre).toList(),
                concentracion.dominios().stream().map(DominioConsolidado::distribucion).toList());

        String descripcion = "Nivel de riesgo intralaboral total";
        distribucionPorGrupo(doc, "Por ciudad de trabajo (regionales)", descripcion, "Ciudad de trabajo", "ciudades",
                concentracion.intralaboralPorCiudad(), NivelRiesgo::nombre);
        distribucionPorGrupo(doc, "Por área", descripcion, "Área", "áreas",
                concentracion.intralaboralPorArea(), NivelRiesgo::nombre);
    }

    /**
     * Frecuencia de síntomas por dimensión (el manual no da niveles por dimensión) y nivel total de
     * síntomas de estrés por ciudad de trabajo y por área.
     */
    private void sintomasEstres(XWPFDocument doc, SintomasEstres sintomas, ConcentracionRiesgo concentracion) {
        if (!sintomas.dimensiones().isEmpty()) {
            subtitulo(doc, "Frecuencia de síntomas por dimensión");
            nota(doc, "El manual del cuestionario de estrés no trae baremos por dimensión, solo para el total; por "
                    + "eso aquí no se muestran niveles sino el porcentaje de respuestas de cada frecuencia en los "
                    + "ítems de la dimensión (" + personas(sintomas.personas()) + "). Cada persona responde varios "
                    + "ítems por dimensión: los porcentajes se calculan sobre todas esas respuestas (personas × "
                    + "ítems), no sobre las personas. Dimensiones ordenadas de la de "
                    + "síntomas más frecuentes a la de menos: primero la de mayor porcentaje de respuestas "
                    + "\"Siempre\"; si hay empate, la de mayor porcentaje en \"Casi siempre\", y así sucesivamente.")
                    .setKeepNext(true);
            unirConElGrafico(doc, tablaFrecuencias(doc, sintomas.dimensiones()));
            graficoFrecuencias(doc, sintomas.dimensiones());
        }

        Function<NivelRiesgo, String> etiqueta = n -> NivelEstres.etiqueta(n.id());
        String descripcion = "Nivel de síntomas de estrés";
        distribucionPorGrupo(doc, "Por ciudad de trabajo (regionales)", descripcion, "Ciudad de trabajo", "ciudades",
                concentracion.estresPorCiudad(), etiqueta);
        distribucionPorGrupo(doc, "Por área", descripcion, "Área", "áreas",
                concentracion.estresPorArea(), etiqueta);
    }

    /** Dimensión, respuestas y el porcentaje de cada frecuencia, con el encabezado en su color. */
    private XWPFTable tablaFrecuencias(XWPFDocument doc, List<FrecuenciaDimension> dimensiones) {
        List<OpcionFrecuencia> opciones = dimensiones.get(0).frecuencias().stream().map(ConteoFrecuencia::opcion).toList();
        int[] anchos = anchosFrecuencias(opciones.size());
        XWPFTable tabla = tabla(doc, 1, anchos);
        XWPFTableRow encabezado = tabla.getRow(0);
        celda(encabezado.getCell(0), "Dimensión", true, ParagraphAlignment.CENTER, GRIS_ENCABEZADO, anchos[0]);
        celda(encabezado.getCell(1), "Respuestas (personas × ítems)", true, ParagraphAlignment.CENTER,
                GRIS_ENCABEZADO, anchos[1]);
        for (int i = 0; i < opciones.size(); i++) {
            celda(encabezado.getCell(2 + i), opciones.get(i).nombre(), true, ParagraphAlignment.CENTER,
                    colorFrecuencia(opciones.get(i)), anchos[2 + i]);
        }
        encabezado.setRepeatHeader(true);
        for (FrecuenciaDimension dimension : dimensiones) {
            XWPFTableRow fila = tabla.createRow();
            celda(fila.getCell(0), dimension.nombre(), false, ParagraphAlignment.LEFT, null, anchos[0]);
            celda(fila.getCell(1), dimension.respuestas() + " (" + dimension.personas() + " × " + dimension.items() + ")",
                    false, ParagraphAlignment.CENTER, null, anchos[1]);
            for (int i = 0; i < dimension.frecuencias().size(); i++) {
                celda(fila.getCell(2 + i), porcentaje(dimension.frecuencias().get(i).porcentaje()), false,
                        ParagraphAlignment.CENTER, null, anchos[2 + i]);
            }
        }
        espacio(doc);
        return tabla;
    }

    /** Dimensión y respuestas fijas; el resto del ancho se reparte entre las frecuencias. */
    private static int[] anchosFrecuencias(int opciones) {
        int[] anchos = new int[opciones + 2];
        anchos[0] = 3000;
        anchos[1] = 1800;
        int resto = ANCHO_TEXTO - anchos[0] - anchos[1];
        for (int i = 2; i < anchos.length; i++) {
            anchos[i] = resto / opciones;
        }
        anchos[anchos.length - 1] += resto % opciones;
        return anchos;
    }

    /** Por cada dimensión, una barra por frecuencia. */
    private void graficoFrecuencias(XWPFDocument doc, List<FrecuenciaDimension> dimensiones) {
        List<OpcionFrecuencia> opciones = dimensiones.get(0).frecuencias().stream().map(ConteoFrecuencia::opcion).toList();
        List<DocxGraficos.Serie> series = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            int indice = i;
            Double[] porcentajes = dimensiones.stream()
                    .map(d -> d.frecuencias().get(indice).porcentaje().doubleValue())
                    .toArray(Double[]::new);
            series.add(new DocxGraficos.Serie(opciones.get(i).nombre(), porcentajes, colorFrecuencia(opciones.get(i))));
        }
        DocxGraficos.barrasAgrupadas(doc, dimensiones.stream().map(FrecuenciaDimension::nombre).toArray(String[]::new),
                series);
    }

    /**
     * Frecuencias con el semáforo: Siempre en rojo … Nunca en verde. Va por id de la opción
     * (1 Siempre … 5 Nunca), el mismo orden de los niveles de riesgo de mayor a menor.
     */
    private static String colorFrecuencia(OpcionFrecuencia opcion) {
        return COLOR_NIVEL.getOrDefault(6L - opcion.id(), GRIS_ENCABEZADO);
    }

    /**
     * Tabla con los grupos del desglose y gráficos de a {@link #GRUPOS_POR_GRAFICO} grupos, para que
     * las barras no queden apiñadas. Cada gráfico lleva su título ("Gráfico 2 de 5 – Cali y Medellín",
     * armado con los datos) y cada grupo su número de personas. Notas de los grupos pequeños que se
     * juntaron en "Otras…" o que se omitieron por confidencialidad.
     *
     * @param plural "ciudades" o "áreas", para las notas
     */
    private void distribucionPorGrupo(XWPFDocument doc, String titulo, String descripcion, String tituloGrupo,
                                      String plural, DesgloseGrupos desglose, Function<NivelRiesgo, String> etiqueta) {
        List<DistribucionGrupo> grupos = desglose.grupos();
        if (grupos.isEmpty() && desglose.omitidos() == 0) {
            return;
        }
        subtitulo(doc, titulo);
        String minimo = personas(AnalizadorInformeEmpresa.MINIMO_PERSONAS_GRUPO);
        if (desglose.omitidos() > 0) {
            nota(doc, (desglose.omitidos() == 1 ? "1 persona no se muestra" : desglose.omitidos() + " personas no se muestran")
                    + " para proteger la confidencialidad: pertenecen a " + plural + " con menos de " + minimo
                    + " evaluadas que, aun juntas, no alcanzan ese mínimo.");
        }
        if (grupos.isEmpty()) {
            return;
        }
        String otras = desglose.agrupados() == 0 ? "" : " \"Otras " + plural + "\" reúne " + desglose.agrupados() + " "
                + plural + " con menos de " + minimo + " evaluadas, para proteger la confidencialidad.";
        nota(doc, descripcion + ", ordenado de mayor a menor número de personas." + otras
                + " Los grupos con pocas personas deben leerse con cautela.").setKeepNext(true);
        unirConElGrafico(doc, tablaPorNivel(doc, ANCHOS_DOMINIOS, new String[]{tituloGrupo, "Personas"},
                grupos.stream()
                        .map(g -> new FilaPorNivel(new String[]{g.grupo(), String.valueOf(g.distribucion().total())},
                                g.distribucion()))
                        .toList(), etiqueta));
        int graficos = (grupos.size() + GRUPOS_POR_GRAFICO - 1) / GRUPOS_POR_GRAFICO;
        for (int i = 0; i < graficos; i++) {
            List<DistribucionGrupo> parte = grupos.subList(i * GRUPOS_POR_GRAFICO,
                    Math.min(grupos.size(), (i + 1) * GRUPOS_POR_GRAFICO));
            graficoPorNivel(doc,
                    parte.stream().map(g -> g.grupo() + " (" + g.distribucion().total() + ")").toList(),
                    parte.stream().map(DistribucionGrupo::distribucion).toList(), etiqueta,
                    tituloGrafico(i + 1, graficos, parte), DocxGraficos.ALTO_COMPACTO);
        }
    }

    /** "Gráfico 2 de 5 – Cali y Medellín"; con un solo gráfico, solo los nombres. */
    private static String tituloGrafico(int numero, int total, List<DistribucionGrupo> grupos) {
        List<String> nombres = grupos.stream().map(DistribucionGrupo::grupo).toList();
        String nombresUnidos = nombres.size() == 1 ? nombres.get(0)
                : String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
        return total == 1 ? nombresUnidos : "Gráfico " + numero + " de " + total + " – " + nombresUnidos;
    }

    /**
     * Mantiene la tabla y el párrafo de espacio que la sigue en la misma página que el gráfico
     * siguiente: Word no deja la tabla al final de una página y su gráfico en la otra.
     */
    private static void unirConElGrafico(XWPFDocument doc, XWPFTable tabla) {
        mantenerConLoSiguiente(tabla);
        doc.getParagraphs().get(doc.getParagraphs().size() - 1).setKeepNext(true);
    }

    /** Tabla y gráfico de los dominios de una forma, del más crítico al menos crítico. */
    private void rankingDominios(XWPFDocument doc, DominiosForma forma) {
        subtitulo(doc, "Ranking de dominios");
        nota(doc, "Dominios " + CRITERIO_RANKING);
        tablaPorNivel(doc, ANCHOS_DOMINIOS, new String[]{"Dominio", "Personas"},
                forma.dominios().stream()
                        .map(d -> new FilaPorNivel(new String[]{d.nombre(), String.valueOf(d.distribucion().total())},
                                d.distribucion()))
                        .toList());
        graficoPorNivel(doc, forma.dominios().stream().map(DistribucionDominio::nombre).toList(),
                forma.dominios().stream().map(DistribucionDominio::distribucion).toList());
    }

    /** Fila de una tabla por nivel: sus textos iniciales y la distribución que da los porcentajes. */
    private record FilaPorNivel(String[] textos, DistribucionNiveles distribucion) {
    }

    /**
     * Tabla con columnas descriptivas (las primeras; la última de ellas, personas) y el porcentaje
     * de cada nivel, con el encabezado de cada nivel en su color.
     */
    private XWPFTable tablaPorNivel(XWPFDocument doc, int[] anchos, String[] titulos, List<FilaPorNivel> filas) {
        return tablaPorNivel(doc, anchos, titulos, filas, NivelRiesgo::nombre);
    }

    private XWPFTable tablaPorNivel(XWPFDocument doc, int[] anchos, String[] titulos, List<FilaPorNivel> filas,
                                    Function<NivelRiesgo, String> etiqueta) {
        List<NivelRiesgo> niveles = filas.get(0).distribucion().niveles().stream().map(ConteoNivel::nivel).toList();
        int primerNivel = titulos.length;
        XWPFTable tabla = tabla(doc, 1, anchos);
        XWPFTableRow encabezado = tabla.getRow(0);
        for (int i = 0; i < titulos.length; i++) {
            celda(encabezado.getCell(i), titulos[i], true, ParagraphAlignment.CENTER, GRIS_ENCABEZADO, anchos[i]);
        }
        for (int i = 0; i < niveles.size(); i++) {
            celda(encabezado.getCell(primerNivel + i), etiqueta.apply(niveles.get(i)), true, ParagraphAlignment.CENTER,
                    COLOR_NIVEL.get(niveles.get(i).id()), anchos[primerNivel + i]);
        }
        encabezado.setRepeatHeader(true);
        for (FilaPorNivel fila : filas) {
            XWPFTableRow filaTabla = tabla.createRow();
            for (int i = 0; i < titulos.length; i++) {
                // Nombres a la izquierda; la última descriptiva es la cantidad de personas, centrada.
                celda(filaTabla.getCell(i), fila.textos()[i], false,
                        i == titulos.length - 1 ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT, null, anchos[i]);
            }
            List<ConteoNivel> conteos = fila.distribucion().niveles();
            for (int i = 0; i < conteos.size(); i++) {
                celda(filaTabla.getCell(primerNivel + i), porcentaje(conteos.get(i).porcentaje()), false,
                        ParagraphAlignment.CENTER, null, anchos[primerNivel + i]);
            }
        }
        espacio(doc);
        return tabla;
    }

    /** Por cada categoría (dominio, ciudad, área), una barra por nivel lado a lado. */
    private void graficoPorNivel(XWPFDocument doc, List<String> categorias, List<DistribucionNiveles> distribuciones) {
        graficoPorNivel(doc, categorias, distribuciones, NivelRiesgo::nombre, null, DocxGraficos.ALTO_AGRUPADO);
    }

    private void graficoPorNivel(XWPFDocument doc, List<String> categorias, List<DistribucionNiveles> distribuciones,
                                 Function<NivelRiesgo, String> etiqueta, String titulo, int alto) {
        List<NivelRiesgo> niveles = distribuciones.get(0).niveles().stream().map(ConteoNivel::nivel).toList();
        List<DocxGraficos.Serie> series = new ArrayList<>();
        for (int i = 0; i < niveles.size(); i++) {
            int indice = i;
            Double[] porcentajes = distribuciones.stream()
                    .map(d -> d.niveles().get(indice).porcentaje().doubleValue())
                    .toArray(Double[]::new);
            series.add(new DocxGraficos.Serie(etiqueta.apply(niveles.get(i)), porcentajes,
                    COLOR_NIVEL.get(niveles.get(i).id())));
        }
        DocxGraficos.barrasAgrupadas(doc, categorias.toArray(String[]::new), series, titulo, alto);
    }

    /** Dimensión con el nombre de su dominio, para el resumen. */
    private record DimensionDeDominio(DistribucionDimension dimension, String dominio) {
    }

    /**
     * Todas las dimensiones de una forma en una tabla, de la más crítica a la menos crítica
     * (empate: en el orden del ranking de dominios). El detalle va en las fichas.
     */
    private void resumenDimensiones(XWPFDocument doc, DominiosForma forma) {
        List<DimensionDeDominio> dimensiones = forma.dominios().stream()
                .flatMap(d -> d.dimensiones().stream().map(dim -> new DimensionDeDominio(dim, d.nombre())))
                .sorted(Comparator.comparing((DimensionDeDominio d) -> d.dimension().distribucion(),
                        DistribucionNiveles.masCriticaPrimero()))
                .toList();
        if (dimensiones.isEmpty()) {
            return;
        }
        subtitulo(doc, "Ranking de dimensiones");
        nota(doc, "Todas las dimensiones de la forma, " + CRITERIO_RANKING
                + " El detalle de cada una está en las fichas por dominio.");
        tablaPorNivel(doc, ANCHOS_RESUMEN, new String[]{"Dimensión", "Dominio", "Personas"},
                dimensiones.stream()
                        .map(d -> new FilaPorNivel(new String[]{d.dimension().nombre(), d.dominio(),
                                String.valueOf(d.dimension().distribucion().total())}, d.dimension().distribucion()))
                        .toList());
    }

    /** Un dominio por página, con una ficha por dimensión en el orden de su ranking. */
    private void fichasDimensiones(XWPFDocument doc, String forma, DistribucionDominio dominio) {
        if (dominio.dimensiones().isEmpty()) {
            return;
        }
        saltoDePagina(doc);
        subtitulo(doc, "Forma " + forma + " – " + dominio.nombre());
        for (DistribucionDimension dimension : dominio.dimensiones()) {
            fichaDimension(doc, forma, dimension,
                    dimension.distribucion().total() != dominio.distribucion().total());
        }
    }

    /**
     * Ficha de una dimensión: título, tabla de una fila (cantidad y % por nivel) y el mismo
     * gráfico de barras por nivel de los resultados globales. Todo queda unido para que la
     * ficha no se parta entre dos páginas.
     */
    private void fichaDimension(XWPFDocument doc, String forma, DistribucionDimension dimension, boolean conFiltro) {
        DistribucionNiveles distribucion = dimension.distribucion();
        tituloFicha(doc, dimension.nombre(), "Forma " + forma + " · " + personas(distribucion.total()));
        if (conFiltro) {
            nota(doc, "Depende de una pregunta filtro (atender clientes o ser jefe): solo cuenta a las personas "
                    + "a quienes les aplica.").setKeepNext(true);
        }

        XWPFTable tabla = tabla(doc, 2, ANCHOS_FICHA);
        List<ConteoNivel> conteos = distribucion.niveles();
        for (int i = 0; i < conteos.size(); i++) {
            ConteoNivel conteo = conteos.get(i);
            celda(tabla.getRow(0).getCell(i), conteo.nivel().nombre(), true, ParagraphAlignment.CENTER,
                    COLOR_NIVEL.get(conteo.nivel().id()), ANCHOS_FICHA[i]);
            celda(tabla.getRow(1).getCell(i), conteo.cantidad() + " (" + porcentaje(conteo.porcentaje()) + ")", false,
                    ParagraphAlignment.CENTER, null, ANCHOS_FICHA[i]);
        }
        mantenerConLoSiguiente(tabla);
        parrafo(doc, ParagraphAlignment.LEFT, 0, 60).setKeepNext(true);

        graficoNiveles(doc, distribucion, c -> c.nivel().nombre());
        parrafo(doc, ParagraphAlignment.LEFT, 0, 160);
    }

    /** Nombre de la dimensión a la izquierda y la forma con las personas a la derecha, sobre una línea. */
    private void tituloFicha(XWPFDocument doc, String nombre, String detalle) {
        XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.LEFT, 120, 80);
        parrafo.setKeepNext(true);
        parrafo.setBorderBottom(Borders.SINGLE);
        CTTabStop tope = parrafo.getCTP().isSetPPr() && parrafo.getCTP().getPPr().isSetTabs()
                ? parrafo.getCTP().getPPr().getTabs().addNewTab()
                : parrafo.getCTP().getPPr().addNewTabs().addNewTab();
        tope.setVal(STTabJc.RIGHT);
        tope.setPos(BigInteger.valueOf(ANCHO_TEXTO));
        texto(parrafo, nombre.toUpperCase(), 10, true, AZUL);
        texto(parrafo, "", 9, false, "555555").addTab();
        texto(parrafo, detalle, 9, false, "555555");
    }

    /** Une cada fila de la tabla con lo que sigue (Word no separa la tabla del gráfico). */
    private static void mantenerConLoSiguiente(XWPFTable tabla) {
        for (XWPFTableRow fila : tabla.getRows()) {
            fila.setCantSplitRow(true);
            for (XWPFTableCell celda : fila.getTableCells()) {
                for (XWPFParagraph parrafo : celda.getParagraphs()) {
                    if (!parrafo.getCTP().isSetPPr()) {
                        parrafo.getCTP().addNewPPr(); // POI no lo crea en setKeepNext
                    }
                    parrafo.setKeepNext(true);
                }
            }
        }
    }

    private static String personas(int cantidad) {
        return cantidad + (cantidad == 1 ? " persona" : " personas");
    }

    private XWPFParagraph nota(XWPFDocument doc, String contenido) {
        XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.BOTH, 0, 120);
        texto(parrafo, "Nota: ", 8.5, true, null);
        texto(parrafo, contenido, 8.5, false, null);
        return parrafo;
    }

    /** Coma decimal, como en el resto de los informes: 66.7 → "66,7 %". */
    private static String porcentaje(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO")));
        return formato.format(valor) + " %";
    }

}
