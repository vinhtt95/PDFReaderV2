package com.vinhtt.PDFReader.viewmodel;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence; // Nhớ import
import com.vinhtt.PDFReader.service.api.GeminiService;
import com.vinhtt.PDFReader.service.api.ITranslationService;
import com.vinhtt.PDFReader.service.pdf.IPdfService;
import com.vinhtt.PDFReader.service.pdf.PdfBoxService;
import com.vinhtt.PDFReader.service.storage.IStorageService;
import com.vinhtt.PDFReader.service.storage.SqliteStorageService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.BreakIterator; // Để tách câu
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainViewModel {
    private final ListProperty<Paragraph> paragraphList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Image> pdfPages = new SimpleListProperty<>(FXCollections.observableArrayList());
    // Property mới: Danh sách câu cho cột phải
    private final ListProperty<Sentence> sentenceList = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<Paragraph> selectedParagraph = new SimpleObjectProperty<>();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");

    private final IPdfService pdfService;
    private final ITranslationService translationService;
    private final IStorageService storageService;

    public MainViewModel() {
        this.pdfService = new PdfBoxService();
        this.translationService = new GeminiService();
        this.storageService = new SqliteStorageService();
    }

    // ... (Giữ nguyên hàm loadPdf)
    public void loadPdf(File file) {
        statusMessage.set("Analyzing PDF & Rendering...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Paragraph> paragraphs = pdfService.parsePdf(file);
                List<BufferedImage> bufferedImages = pdfService.renderPdfPages(file);
                List<Image> fxImages = new ArrayList<>();
                for (BufferedImage bi : bufferedImages) {
                    fxImages.add(SwingFXUtils.toFXImage(bi, null));
                }
                Platform.runLater(() -> {
                    paragraphList.setAll(paragraphs);
                    pdfPages.setAll(fxImages);
                });
                String dbPath = file.getParent() + File.separator + file.getName() + ".meta.db";
                storageService.initDatabase(dbPath);
                return null;
            }
        };
        task.setOnSucceeded(e -> statusMessage.set("File loaded: " + file.getName()));
        task.setOnFailed(e -> {
            statusMessage.set("Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    public void translateParagraph(Paragraph p) {
        statusMessage.set("Translating ID " + p.hashCode() + "...");
        translationService.translate(p.getOriginalText())
                .thenAccept(translated -> Platform.runLater(() -> {
                    p.setTranslatedText(translated);
                    int index = paragraphList.indexOf(p);
                    if (index >= 0) paragraphList.set(index, p);
                    statusMessage.set("Translation complete.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusMessage.set("Translation failed: " + ex.getMessage()));
                    return null;
                });
    }

    // Logic mới: Tách câu
    public void loadSentencesFor(Paragraph p) {
        if (p == null) {
            sentenceList.clear();
            return;
        }

        List<Sentence> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        String text = p.getOriginalText();
        iterator.setText(text);

        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentenceText = text.substring(start, end).trim();
            if (!sentenceText.isEmpty()) {
                sentences.add(new Sentence(sentenceText, null));
            }
        }
        sentenceList.setAll(sentences);
    }

    // Logic mới: Phân tích ngữ pháp câu
    public void analyzeSentence(Sentence s) {
        statusMessage.set("Analyzing grammar...");
        translationService.analyze(s.getOriginal())
                .thenAccept(result -> Platform.runLater(() -> {
                    s.setAnalysis(result);
                    // Trigger update ListView
                    int index = sentenceList.indexOf(s);
                    if (index >= 0) sentenceList.set(index, s);
                    statusMessage.set("Analysis complete.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusMessage.set("Analysis error: " + ex.getMessage()));
                    return null;
                });
    }

    // Getters
    public ObservableList<Paragraph> getParagraphList() { return paragraphList.get(); }
    public ListProperty<Paragraph> paragraphListProperty() { return paragraphList; }
    public ObservableList<Image> getPdfPages() { return pdfPages.get(); }
    public ListProperty<Image> pdfPagesProperty() { return pdfPages; }

    public ObservableList<Sentence> getSentenceList() { return sentenceList.get(); }
    public ListProperty<Sentence> sentenceListProperty() { return sentenceList; }

    public ObjectProperty<Paragraph> selectedParagraphProperty() { return selectedParagraph; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}