package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
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
    public String calculateFileHash(File file) {
        // Simplified hash logic for demo
        return file.getName() + "_" + file.length();
    }

    // Inner class to handle text extraction logic
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

            // Simple logic: if Y distance is large, treat as new paragraph
            if (Math.abs(y - currentY) > 20 && currentParagraph.length() > 0) {
                saveParagraph();
            }

            currentParagraph.append(text).append(" ");
            currentY = y;
        }

        private void saveParagraph() {
            String text = currentParagraph.toString().trim();
            if (!text.isEmpty()) {
                outputList.add(new Paragraph(idCounter++, text, currentY));
            }
            currentParagraph.setLength(0);
        }

        // Flush remaining text at end of page
        @Override
        protected void writePage() throws IOException {
            super.writePage();
            saveParagraph();
        }
    }
}