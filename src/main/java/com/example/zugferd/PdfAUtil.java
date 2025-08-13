package com.example.zugferd;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.TimeZone;

public final class PdfAUtil {
    private PdfAUtil() {}

    /** Normales PDF -> PDF/A-3B (sRGB ICC + XMP) */
    public static Path toPDFA3(Path in, Path out, Path iccPath) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.toFile())) {
            // sRGB OutputIntent
            try (InputStream colorProfile = Files.newInputStream(iccPath)) {
                PDOutputIntent oi = new PDOutputIntent(doc, colorProfile);
                oi.setInfo("sRGB IEC61966-2.1");
                oi.setOutputCondition("sRGB IEC61966-2.1");
                oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
                oi.setRegistryName("http://www.color.org");
                doc.getDocumentCatalog().addOutputIntent(oi);
            }

            // XMP: PDF/A-3B Kennzeichnung
            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            PDFAIdentificationSchema id = xmp.createAndAddPDFAIdentificationSchema();
            id.setPart(3);            // PDF/A-3
            id.setConformance("B");   // Level B

            XMPBasicSchema xmpBasic = xmp.createAndAddXMPBasicSchema();
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            xmpBasic.setCreateDate(now);
            xmpBasic.setModifyDate(now);
            xmpBasic.setMetadataDate(now);
            xmpBasic.setCreatorTool("ZUGFeRD-POC");

            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.addCreator("ZUGFeRD-POC");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new XmpSerializer().serialize(xmp, baos, true);

            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(baos.toByteArray());
            PDDocumentCatalog cat = doc.getDocumentCatalog();
            cat.setMetadata(metadata);

            PDDocumentInformation info = doc.getDocumentInformation();
            if (info == null) info = new PDDocumentInformation();
            info.setProducer("PDFBox");
            info.setCreator("ZUGFeRD-POC");
            doc.setDocumentInformation(info);

            doc.save(out.toFile());
        }
        return out;
    }
}
