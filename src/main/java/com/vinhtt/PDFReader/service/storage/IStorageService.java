package com.vinhtt.PDFReader.service.storage;

import com.vinhtt.PDFReader.model.Paragraph;
import java.util.List;

public interface IStorageService {
    void initDatabase(String dbPath);
    void saveParagraphs(List<Paragraph> paragraphs);
    void updateTranslation(int id, String translatedText);
    List<Paragraph> getParagraphs();
}