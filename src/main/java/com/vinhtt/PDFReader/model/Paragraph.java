package com.vinhtt.PDFReader.model;

public class Paragraph {
    private int id;
    private String originalText;
    private String translatedText;
    private float yPosition;

    public Paragraph(int id, String originalText, float yPosition) {
        this.id = id;
        this.originalText = originalText;
        this.yPosition = yPosition;
    }

    // Getters and Setters
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    @Override
    public String toString() {
        // Hiển thị text trên ListView tạm thời
        return translatedText != null ? translatedText : originalText;
    }
}