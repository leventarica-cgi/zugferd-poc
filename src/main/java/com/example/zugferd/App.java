
package com.example.zugferd;

import org.mustangproject.ZUGFeRD.IZUGFeRDExporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromPDFA;
import org.mustangproject.ZUGFeRD.Profiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
  public static void main(String[] args) throws Exception {
    CanonicalInvoice invoice = SampleDataLoader.loadFromJson("/sample-invoice.json");

    Path buildDir = Paths.get("build");
    Files.createDirectories(buildDir);
    Path plainPdf = buildDir.resolve("rechnung.pdf");
    Path pdfa3    = buildDir.resolve("rechnung_pdfa3.pdf");
    Path outPdf   = buildDir.resolve("rechnung_zugferd.pdf");

    PdfRenderer.render(plainPdf, invoice);

    // macOS default ICC profile, adjust on other OS
    Path icc = Paths.get("/System/Library/ColorSync/Profiles/sRGB Profile.icc");
    PdfAUtil.toPDFA3(plainPdf, pdfa3, icc);    // convert to PDF/A-3

    var mustangInvoice = MustangMapper.toZugferd(invoice);

    IZUGFeRDExporter exporter = new ZUGFeRDExporterFromPDFA()
            .load(pdfa3.toString())     // load the PDF/A-3 file
            .setProducer("PDFBox")
            .setCreator("ZUGFeRD-POC")
            .setProfile(Profiles.getByName("EN16931"));

    exporter.setTransaction(mustangInvoice);
    exporter.export(outPdf.toString());

    System.out.println("Fertig. Artefakte:");
    System.out.println(" - " + plainPdf.toAbsolutePath());
    System.out.println(" - " + pdfa3.toAbsolutePath());
    System.out.println(" - " + outPdf.toAbsolutePath());
  }
}
