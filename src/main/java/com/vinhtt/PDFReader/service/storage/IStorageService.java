package com.vinhtt.PDFReader.service.storage;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
import java.util.List;

public interface IStorageService {
    void initDatabase(String dbPath);

    // Paragraphs
    boolean hasData(); // Kiểm tra xem đã từng parse chưa
    void saveParagraphs(List<Paragraph> paragraphs);
    void updateParagraphTranslation(int id, String translatedText);
    List<Paragraph> getAllParagraphs();

    // Sentences
    List<Sentence> getSentencesByParagraphId(int paragraphId);
    void saveSentences(List<Sentence> sentences);
    void updateSentenceAnalysis(int id, String analysisJson);
}