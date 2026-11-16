package com.civis.api;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController @RequestMapping("/api/admin")
class ReportController {
 private final CitizenRepository citizens;private final ApiService auth;
 @org.springframework.beans.factory.annotation.Value("${civis.demo-mode:false}") boolean demo;
 ReportController(CitizenRepository citizens,ApiService auth){this.citizens=citizens;this.auth=auth;}
 @GetMapping(value="/report.pdf",produces="application/pdf")
 ResponseEntity<byte[]> report(@RequestHeader("Authorization") String token,@RequestParam(required=false) Boolean required) throws IOException {
  auth.requireAdmin(auth.require(token));
  List<Citizen> records=citizens.findAll().stream().filter(c->required==null || c.required==required).toList();
  try(PDDocument doc=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   PDPage page=new PDPage();doc.addPage(page);PDPageContentStream content=new PDPageContentStream(doc,page);
   try{
    content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),16);content.newLineAtOffset(48,750);content.showText("CIVIS - Reporte de ciudadanos"+(demo?" (DEMO)":" (Registro interno)"));content.endText();
    int y=720;for(Citizen c:records){if(y<55){content.close();page=new PDPage();doc.addPage(page);content=new PDPageContentStream(doc,page);y=750;}
     String line=(c.documentId+" | "+ascii(c.firstName)+" "+ascii(c.lastName)+" | "+(c.required?"REQUERIDO":"NO REQUERIDO"));
     content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),10);content.newLineAtOffset(48,y);content.showText(line.substring(0,Math.min(line.length(),94)));content.endText();y-=18;
    }
   }finally{content.close();}
   doc.save(output);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=civis-reporte.pdf").body(output.toByteArray());
  }
 }
 private static String ascii(String s){return s==null?"":java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]","");}
}
