package com.example.zugferd;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PdfRenderer {
    private PdfRenderer() {}

    public static Path render(Path out, CanonicalInvoice inv) throws IOException {
        Files.createDirectories(out.getParent());
        Locale locale = Locale.GERMANY;
        NumberFormat money = NumberFormat.getCurrencyInstance(locale);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        BigDecimal netTotal = inv.lines().stream()
                .map(li -> li.netUnitPrice().multiply(li.qty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = netTotal.multiply(inv.taxRate());
        BigDecimal gross = netTotal.add(tax);

        try (PDDocument doc = new PDDocument();
             InputStream regularIs = PdfRenderer.class.getResourceAsStream("/fonts/DejaVuSans.ttf");
             InputStream boldIs    = PdfRenderer.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf")) {

            // PDF/A-3 basiert auf PDF 1.7
            doc.setVersion(1.7f);

            if (regularIs == null || boldIs == null) {
                throw new IOException("Embedded fonts not found under /fonts/. Expected DejaVuSans.ttf and DejaVuSans-Bold.ttf");
            }

            // Eingebettete Unicode-Schriften (PDF/A-konform)
            PDFont FONT = PDType0Font.load(doc, regularIs, true);
            PDFont FONT_BOLD = PDType0Font.load(doc, boldIs, true);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 800f;
                float left = 50f;

                // Seller (Absender)
                write(cs, FONT_BOLD, 12, left, y, inv.seller().name());
                y -= 12;
                write(cs, FONT, 10, left, y, inv.seller().street());
                y -= 12;
                write(cs, FONT, 10, left, y, inv.seller().zip() + " " + inv.seller().city());
                y -= 18;

                // Buyer
                write(cs, FONT_BOLD, 12, left, y, "Firma");
                y -= 12;
                write(cs, FONT, 10, left, y, inv.buyer().name());
                y -= 12;
                write(cs, FONT, 10, left, y, inv.buyer().street());
                y -= 12;
                write(cs, FONT, 10, left, y, inv.buyer().zip() + " " + inv.buyer().city());

                // Date (top right)
                write(cs, FONT, 10, 450, 720, df.format(inv.issueDate()));

                // Optional notes
                y -= 40;
                if (inv.notes() != null && !inv.notes().isBlank()) {
                    write(cs, FONT_BOLD, 11, left, y, inv.notes());
                    y -= 18;
                }

                // Invoice number
                write(cs, FONT_BOLD, 12, left, y, "Rechnung-Nr. " + inv.number());
                y -= 30;

                // Positions header
                write(cs, FONT_BOLD, 11, left, y, "Wir berechnen...");
                y -= 18;

                // Table header
                write(cs, FONT_BOLD, 10, left, y, "Bezeichnung");
                write(cs, FONT_BOLD, 10, 330, y, "Menge");
                write(cs, FONT_BOLD, 10, 380, y, "Einheit");
                write(cs, FONT_BOLD, 10, 440, y, "Einzelpreis");
                write(cs, FONT_BOLD, 10, 520, y, "Betrag");
                y -= 12;

                // Lines
                for (var li : inv.lines()) {
                    write(cs, FONT, 10, left, y, li.name());
                    write(cs, FONT, 10, 330, y, li.qty().stripTrailingZeros().toPlainString());
                    write(cs, FONT, 10, 380, y, li.unit());
                    write(cs, FONT, 10, 440, y, money.format(li.netUnitPrice()));
                    write(cs, FONT, 10, 520, y, money.format(li.netUnitPrice().multiply(li.qty())));
                    y -= 12;
                }

                // Totals
                y -= 20;
                write(cs, FONT_BOLD, 11, left, y, "Nettobetrag:");
                write(cs, FONT_BOLD, 11, 520, y, money.format(netTotal));
                y -= 14;
                write(cs, FONT, 10, left, y, "zzgl. USt (" + inv.taxRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + " %):");
                write(cs, FONT, 10, 520, y, money.format(tax));
                y -= 8;
                drawLine(cs, left,  y - 2, 545, y - 2);
                y -= 14;
                write(cs, FONT_BOLD, 12, left, y, "Gesamtbetrag");
                write(cs, FONT_BOLD, 12, 520, y, money.format(gross));

                // Payment terms
                y -= 24;
                write(cs, FONT, 10, left, y, inv.paymentTerms() != null ? inv.paymentTerms() : "Der Rechnungsbetrag ist sofort zur Zahlung fällig.");
                y -= 14;
                write(cs, FONT, 10, left, y, "Bitte überweisen Sie auf unser nachstehendes Konto.");
                y -= 20;
                write(cs, FONT, 10, left, y, "Bei Rückfragen stehen wir Ihnen gerne zur Verfügung.");
                y -= 14;
                write(cs, FONT, 10, left, y, "Das Leistungsdatum entspricht dem Rechnungsdatum.");

                // Footer bank info
                write(cs, FONT_BOLD, 10, left, 80, (inv.bankName() != null ? ("Bankverbindung " + inv.bankName()) : "Bankverbindung"));
                write(cs, FONT, 10, left, 66, "IBAN " + (inv.iban() != null ? inv.iban() : "DE00 0000 0000 0000 0000 00") + "   BIC " + (inv.bic() != null ? inv.bic() : "DEUTDEFFXXX"));
            }

            doc.save(out.toFile());
        }
        return out;
    }

    private static void write(PDPageContentStream cs, PDFont font, int fontSize, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }
}
