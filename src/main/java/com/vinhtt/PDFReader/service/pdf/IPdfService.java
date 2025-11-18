// File: src/main/java/com/vinhtt/PDFReader/service/pdf/IPdfService.java
package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface IPdfService {
    // Parse text (giữ nguyên)
    List<Paragraph> parsePdf(File file) throws Exception;

    // Mới: Load file PDF để giữ trong Memory
    PDDocument loadDocument(File file) throws Exception;

    // Mới: Render trang cụ thể với chất lượng cao
    BufferedImage renderPage(PDDocument document, int pageIndex, float scale) throws Exception;

    String calculateFileHash(File file);
}