package com.vinhtt.PDFReader.model;

public class Sentence {
    private int id;             // ID trong DB
    private int paragraphId;    // Foreign key
    private String original;
    private String analysis;

    // Constructor dùng khi tạo mới từ tách chuỗi (chưa có ID)
    public Sentence(int paragraphId, String original, String analysis) {
        this.paragraphId = paragraphId;
        this.original = original;
        this.analysis = analysis;
    }

    // Constructor dùng khi load từ DB (đã có ID)
    public Sentence(int id, int paragraphId, String original, String analysis) {
        this.id = id;
        this.paragraphId = paragraphId;
        this.original = original;
        this.analysis = analysis;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getParagraphId() { return paragraphId; }
    public void setParagraphId(int paragraphId) { this.paragraphId = paragraphId; }

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
}