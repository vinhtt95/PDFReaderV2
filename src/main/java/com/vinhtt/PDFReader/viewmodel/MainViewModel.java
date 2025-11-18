package com.vinhtt.PDFReader.viewmodel;

import com.vinhtt.PDFReader.model.Paragraph;
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

import java.io.File;
import java.util.List;

public class MainViewModel {
    // Properties bound to View
    private final ListProperty<Paragraph> paragraphList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Paragraph> selectedParagraph = new SimpleObjectProperty<>();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");

    // Services
    private final IPdfService pdfService;
    private final ITranslationService translationService;
    private final IStorageService storageService;

    public MainViewModel() {
        this.pdfService = new PdfBoxService();
        this.translationService = new GeminiService();
        this.storageService = new SqliteStorageService();
    }

    public void loadPdf(File file) {
        statusMessage.set("Loading PDF...");
        Task<List<Paragraph>> task = new Task<>() {
            @Override
            protected List<Paragraph> call() throws Exception {
                // 1. Parse PDF
                List<Paragraph> paragraphs = pdfService.parsePdf(file);

                // 2. Init DB and Cache (Simple logic)
                String dbPath = file.getParent() + File.separator + file.getName() + ".meta.db";
                storageService.initDatabase(dbPath);
                // storageService.saveParagraphs(paragraphs); // Uncomment to enable save

                return paragraphs;
            }
        };

        task.setOnSucceeded(e -> {
            paragraphList.setAll(task.getValue());
            statusMessage.set("PDF Loaded: " + file.getName());
        });

        task.setOnFailed(e -> {
            statusMessage.set("Error loading PDF");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    public void translateParagraph(Paragraph p) {
        statusMessage.set("Translating...");
        translationService.translate(p.getOriginalText())
                .thenAccept(translated -> Platform.runLater(() -> {
                    p.setTranslatedText(translated);
                    // Refresh list view item logic usually handled by property extractor or force refresh
                    int index = paragraphList.indexOf(p);
                    if (index >= 0) {
                        paragraphList.set(index, p); // Trigger update
                    }
                    statusMessage.set("Translation complete.");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusMessage.set("Translation failed: " + ex.getMessage()));
                    return null;
                });
    }

    // Getters for properties
    public ObservableList<Paragraph> getParagraphList() { return paragraphList.get(); }
    public ListProperty<Paragraph> paragraphListProperty() { return paragraphList; }

    public ObjectProperty<Paragraph> selectedParagraphProperty() { return selectedParagraph; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}