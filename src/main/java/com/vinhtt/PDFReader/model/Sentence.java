package com.vinhtt.PDFReader.model;

public class Sentence {
    private String original;
    private String analysis; // JSON String lưu kết quả phân tích ngữ pháp

    public Sentence(String original, String analysis) {
        this.original = original;
        this.analysis = analysis;
    }

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
}