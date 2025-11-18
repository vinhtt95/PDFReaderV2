package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
    public List<BufferedImage> renderPdfPages(File file) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // Scale 1.5f ~ 108 DPI
                images.add(renderer.renderImage(i, 1.5f));
            }
        }
        return images;
    }

    @Override
    public String calculateFileHash(File file) {
        return file.getName() + "_" + file.length();
    }

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