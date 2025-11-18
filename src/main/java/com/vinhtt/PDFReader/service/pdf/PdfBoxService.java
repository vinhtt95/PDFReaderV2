// File: src/main/java/com/vinhtt/PDFReader/service/pdf/PdfBoxService.java
package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfBoxService implements IPdfService {

    @Override
    public List<Paragraph> parsePdf(File file) throws Exception {
        // ... (Giữ nguyên code parse text cũ của bạn ở đây)
        List<Paragraph> paragraphs = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            CustomPDFTextStripper stripper = new CustomPDFTextStripper(paragraphs);
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            stripper.getText(document);
        }
        return paragraphs;
    }

    @Override
    public PDDocument loadDocument(File file) throws Exception {
        // Mở file PDF và trả về đối tượng document để ViewModel quản lý vòng đời
        return Loader.loadPDF(file);
    }

    @Override
    public BufferedImage renderPage(PDDocument document, int pageIndex, float scale) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        // Scale 3.0f ~ 216 DPI (Rất nét trên màn hình Retina/HiDPI)
        // ImageType.RGB giúp giảm dung lượng bộ nhớ so với ARGB nếu không cần trong suốt
        return renderer.renderImage(pageIndex, scale, ImageType.RGB);
    }

    @Override
    public String calculateFileHash(File file) {
        return file.getName() + "_" + file.length();
    }

    // ... (Giữ nguyên class CustomPDFTextStripper cũ của bạn)
    private static class CustomPDFTextStripper extends PDFTextStripper {
        private final List<Paragraph> outputList;
        private StringBuilder currentParagraph = new StringBuilder();
        private float currentY = 0;
        private int idCounter = 0;

        public CustomPDFTextStripper(List<Paragraph> outputList) throws IOException {
            this.outputList = outputList;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty()) return;
            float y = textPositions.get(0).getY();

            // Logic ghép đoạn văn
            if (Math.abs(y - currentY) > 20 && currentParagraph.length() > 0) {
                saveParagraph();
            }
            currentParagraph.append(text).append(" ");
            currentY = y;
        }

        private void saveParagraph() {
            String text = currentParagraph.toString().trim();
            if (!text.isEmpty()) {
                // Lấy Page Index hiện tại (PDFBox tính từ 1, ta trừ 1 để ra index 0-based)
                int pageIndex = getCurrentPageNo() - 1;
                outputList.add(new Paragraph(idCounter++, pageIndex, text, currentY));
            }
            currentParagraph.setLength(0);
        }

        @Override
        protected void writePage() throws IOException {
            super.writePage();
            saveParagraph(); // Lưu đoạn cuối cùng của trang
        }
    }
}