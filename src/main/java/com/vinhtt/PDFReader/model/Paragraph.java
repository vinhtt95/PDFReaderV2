package com.vinhtt.PDFReader.model;

public class Paragraph {
    private int id;
    private int pageIndex; // Thêm trường này (bắt đầu từ 0)
    private String originalText;
    private String translatedText;
    private float yPosition;

    public Paragraph(int id, int pageIndex, String originalText, float yPosition) {
        this.id = id;
        this.pageIndex = pageIndex;
        this.originalText = originalText;
        this.yPosition = yPosition;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPageIndex() { return pageIndex; }
    public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public float getYPosition() { return yPosition; }
    public void setYPosition(float yPosition) { this.yPosition = yPosition; }

    @Override
    public String toString() {
        return translatedText != null ? translatedText : originalText;
    }
}