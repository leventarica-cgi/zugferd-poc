package com.example.zugferd;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdfRenderer {
    private PdfRenderer() {}

    public static Path render(Path out, CanonicalInvoice inv) throws Exception {
        Files.createDirectories(out.getParent());

        // 1) Thymeleaf-Engine
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        // 2) Calculate Values and put into context
        java.math.BigDecimal netTotal = inv.lines().stream()
                .map(li -> li.netUnitPrice().multiply(li.qty()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal tax = netTotal.multiply(inv.taxRate());
        java.math.BigDecimal gross = netTotal.add(tax);

        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String issueDateFmt = inv.issueDate().format(df);

        Context ctx = new Context(java.util.Locale.GERMANY);
        ctx.setVariable("inv", inv);
        ctx.setVariable("netTotal", netTotal);
        ctx.setVariable("tax", tax);
        ctx.setVariable("gross", gross);
        ctx.setVariable("issueDateFmt", issueDateFmt);

        // 3) create HTML
        String html = engine.process("invoice", ctx);

        // 4) HTML -> PDF (Flying Saucer)
        String baseUri = PdfRenderer.class.getResource("/").toURI().toString();
        try (OutputStream os = Files.newOutputStream(out)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html, baseUri);
            renderer.layout();
            renderer.createPDF(os);
        }

        return out;
    }
}
