package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformePort;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

/**
 * Informe individual en PDF: plantilla {@code templates/informes/informe-evaluado.html}
 * (XHTML bien formado, como exige OpenHTMLtoPDF) rellenada con Thymeleaf.
 */
@Component
public class PdfInformeEvaluadoGenerator implements GeneradorDocumentoInformePort {

    private static final String PLANTILLA = "informe-evaluado";

    private final InformeVistaMapper vistaMapper;
    private final TemplateEngine templateEngine;

    public PdfInformeEvaluadoGenerator(InformeVistaMapper vistaMapper) {
        this.vistaMapper = vistaMapper;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/informes/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    @Override
    public FormatoDocumento formato() {
        return FormatoDocumento.PDF;
    }

    @Override
    public byte[] generar(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion) {
        Context contexto = new Context();
        contexto.setVariable("informe", vistaMapper.toVista(informe, firmas, fechaElaboracion));
        String html = templateEngine.process(PLANTILLA, contexto);

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF del informe", e);
        }
    }

}
