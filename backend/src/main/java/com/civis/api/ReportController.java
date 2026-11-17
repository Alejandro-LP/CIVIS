package com.civis.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
class ReportController {
    private final CitizenRepository citizens;
    private final ApiService auth;
    @Value("${civis.demo-mode:false}") boolean demo;

    ReportController(CitizenRepository citizens, ApiService auth) {
        this.citizens = citizens;
        this.auth = auth;
    }

    @GetMapping(value = "/report.pdf", produces = "application/pdf")
    ResponseEntity<byte[]> report(@RequestHeader("Authorization") String token,
                                 @RequestParam(required = false) Boolean required) throws IOException {
        auth.requireAdmin(auth.require(token));
        List<Citizen> records = citizens.findAll(org.springframework.data.domain.Sort.by("documentId"))
            .stream().filter(c -> required == null || c.required == required).toList();
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        float[] positions = {40, 150, 370, 590};
        float[] widths = {100, 210, 210, 190};
        String filter = required == null ? "Todos" : required ? "Requeridos" : "No requeridos";
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = newPage(document);
            PDPageContentStream content = new PDPageContentStream(document, page);
            try {
                float y = header(content, regular, bold, filter, records.size());
                if (records.isEmpty()) text(content, regular, 11, 40, y, "Sin ciudadanos para el filtro seleccionado.");
                for (Citizen citizen : records) {
                    String[] values = {citizen.documentId, citizen.firstName, citizen.lastName,
                                       citizen.required ? "REQUERIDO" : "NO REQUERIDO"};
                    List<List<String>> columns = new ArrayList<>();
                    int lineCount = 1;
                    for (int column = 0; column < values.length; column++) {
                        List<String> lines = wrap(values[column], regular, widths[column]);
                        columns.add(lines);
                        lineCount = Math.max(lineCount, lines.size());
                    }
                    float rowHeight = lineCount * 14 + 12;
                    if (y - rowHeight < 55) {
                        content.close();
                        page = newPage(document);
                        content = new PDPageContentStream(document, page);
                        y = header(content, regular, bold, filter, records.size());
                    }
                    for (int column = 0; column < columns.size(); column++) {
                        for (int line = 0; line < columns.get(column).size(); line++) {
                            text(content, regular, 10, positions[column], y - line * 14, columns.get(column).get(line));
                        }
                    }
                    y -= rowHeight;
                }
            } finally {
                content.close();
            }
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                try (PDPageContentStream footer = new PDPageContentStream(document, document.getPage(index),
                     PDPageContentStream.AppendMode.APPEND, true)) {
                    text(footer, regular, 9, 40, 28, "CIVIS | Página " + (index + 1) + " de " + document.getNumberOfPages());
                }
            }
            document.save(output);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=civis-reporte.pdf").body(output.toByteArray());
        }
    }

    private PDPage newPage(PDDocument document) {
        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);
        return page;
    }

    private float header(PDPageContentStream content, PDFont regular, PDFont bold,
                         String filter, int count) throws IOException {
        text(content, bold, 18, 40, 548, "CIVIS Reporte de ciudadanos");
        text(content, regular, 10, 40, 525, (demo ? "Laboratorio con datos ficticios" : "Registro interno")
            + " | Fecha " + LocalDate.now() + " | Filtro " + filter + " | Registros " + count);
        text(content, bold, 10, 40, 495, "Identificación");
        text(content, bold, 10, 150, 495, "Nombres");
        text(content, bold, 10, 370, 495, "Apellidos");
        text(content, bold, 10, 590, 495, "Estado registrado");
        return 473;
    }

    private static void text(PDPageContentStream content, PDFont font, int size,
                             float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private static List<String> wrap(String value, PDFont font, float width) throws IOException {
        String cleaned = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ");
        StringBuilder supported = new StringBuilder();
        for (int offset = 0; offset < cleaned.length();) {
            int codePoint = cleaned.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            offset += Character.charCount(codePoint);
            try {
                font.getStringWidth(character);
                supported.append(character);
            } catch (IllegalArgumentException unsupported) {
                supported.append('?');
            }
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (char character : supported.toString().toCharArray()) {
            if (font.getStringWidth(line.toString() + character) * 10 / 1000 > width && !line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
        }
        lines.add(line.toString());
        return lines;
    }
}
