# ZUGFeRD / Factur-X Proof of Concept

## Beschreibung
Diese Demo-App erstellt auf der Grundlage von Beispieldaten eine Rechnung nach dem ZUGFeRD-Standard. Eine PDF wird erstellt, zu **PDF/A-3B** konvertiert und mit einem **ZUGFeRD/Factur-X EN16931 XML**-Anhang versehen.

---

## Tech-Stack
- **Java**: 21 (Temurin/Corretto)
- **Buildsystem**: Gradle 8.13 (Wrapper)
- **Libraries**:
    - `org.apache.pdfbox:pdfbox:3.0.5` – PDF-Erzeugung
    - `org.apache.pdfbox:xmpbox:3.0.5` – XMP-Metadaten (PDF/A)
    - `org.mustangproject:library:2.19.0` – ZUGFeRD/Factur-X-Export
    - `com.fasterxml.jackson.core:jackson-databind:2.17.1` – JSON-Verarbeitung
    - `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1` – Java Time-API in Jackson
    - `org.slf4j:slf4j-simple:2.0.13` – Logging
- **Fonts**: Eingebettete TTF-Fonts (`DejaVuSans.ttf`, `DejaVuSans-Bold.ttf`) für PDF/A-Konformität
- **ICC-Profile**: sRGB (macOS: `/System/Library/ColorSync/Profiles/sRGB Profile.icc`)

---

## Projektaufbau
```
src/main/java/com/example/zugferd/
├── App.java               # Main
├── PdfRenderer.java       # Erstellt PDF mit eingebetteten Fonts
├── PdfAUtil.java          # Konvertiert PDF → PDF/A-3B (ICC + XMP)
├── MustangMapper.java     # Mappt Domainmodell → Mustang Invoice
├── CanonicalInvoice.java  # Domainmodell
└── SampleDataLoader.java  # Lädt Beispielrechnung aus JSON

src/main/resources/
├── fonts/DejaVuSans.ttf
├── fonts/DejaVuSans-Bold.ttf
└── sample-invoice.json
```

---

## Ablauf & Funktionsweise
1. **Daten laden**  
   `CanonicalInvoice` wird aus `sample-invoice.json` mit Jackson eingelesen.

2. **PDF erstellen (PDFBox)**
    - `PdfRenderer` zeichnet die Rechnung auf Basis der geladenen Daten.
    - Fonts werden mit `PDType0Font` eingebettet (PDF/A-Anforderung).
    - **Ergebnis:** `build/rechnung.pdf` (PDF 1.7)

3. **PDF/A-3B konvertieren (PDFBox + XMPBox)**
    - `PdfAUtil` fügt sRGB-ICC-Profil hinzu.
    - Setzt XMP-Metadaten (`pdfaid:part=3`, `pdfaid:conformance=B`).
    - **Ergebnis:** `build/rechnung_pdfa3.pdf`

4. **ZUGFeRD-Daten einbetten (MustangProject)**
    - `MustangMapper` wandelt `CanonicalInvoice` in `org.mustangproject.Invoice`.
    - `ZUGFeRDExporterFromPDFA` lädt die PDF/A-3B-Datei.
    - XML-Anhang wird im Profil **EN16931** eingebettet.
    - **Ergebnis:** `build/rechnung_zugferd.pdf` (PDF/A-3B mit XML)

---

## Setup & Ausführung

### 1) Gradle-Wrapper an Zielsystem anpassen
```bash
gradle wrapper --gradle-version 8.13
./gradlew -v
```

### 2) Projekt starten
```bash
./gradlew clean run
```

### 3) Artefakte
- `build/rechnung.pdf` – PDF ohne PDF/A-Konvertierung
- `build/rechnung_pdfa3.pdf` – PDF/A-3B ohne ZUGFeRD-Daten
- `build/rechnung_zugferd.pdf` – PDF/A-3B mit ZUGFeRD-XML  
