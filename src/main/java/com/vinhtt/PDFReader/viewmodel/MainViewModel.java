package com.vinhtt.PDFReader.viewmodel;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
import com.vinhtt.PDFReader.service.api.GeminiService;
import com.vinhtt.PDFReader.service.api.ITranslationService;
import com.vinhtt.PDFReader.service.pdf.IPdfService;
import com.vinhtt.PDFReader.service.pdf.PdfBoxService;
import com.vinhtt.PDFReader.service.storage.IStorageService;
import com.vinhtt.PDFReader.service.storage.SqliteStorageService;
import com.vinhtt.PDFReader.util.ConfigLoader;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainViewModel {
    private final ListProperty<Paragraph> paragraphList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Image> pdfPages = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Sentence> sentenceList = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<Paragraph> selectedParagraph = new SimpleObjectProperty<>();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final IntegerProperty appFontSize = new SimpleIntegerProperty(ConfigLoader.getFontSize());

    private final IPdfService pdfService;
    private final ITranslationService translationService;
    private final IStorageService storageService;

    public MainViewModel() {
        this.pdfService = new PdfBoxService();
        this.translationService = new GeminiService();
        this.storageService = new SqliteStorageService();
    }

    public void loadPdf(File file) {
        statusMessage.set("Initializing Storage...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String dbPath = file.getParent() + File.separator + file.getName() + ".meta.db";
                storageService.initDatabase(dbPath);

                List<BufferedImage> bufferedImages = pdfService.renderPdfPages(file);
                List<Image> fxImages = new ArrayList<>();
                for (BufferedImage bi : bufferedImages) {
                    fxImages.add(SwingFXUtils.toFXImage(bi, null));
                }
                Platform.runLater(() -> pdfPages.setAll(fxImages));

                if (storageService.hasData()) {
                    updateMessage("Loading from Cache...");
                    List<Paragraph> cachedParagraphs = storageService.getAllParagraphs();
                    Platform.runLater(() -> paragraphList.setAll(cachedParagraphs));
                } else {
                    updateMessage("Parsing PDF...");
                    List<Paragraph> paragraphs = pdfService.parsePdf(file);
                    storageService.saveParagraphs(paragraphs);
                    Platform.runLater(() -> paragraphList.setAll(paragraphs));
                }
                return null;
            }
        };

        task.messageProperty().addListener((obs, old, msg) -> statusMessage.set(msg));
        task.setOnSucceeded(e -> statusMessage.set("File loaded: " + file.getName()));
        task.setOnFailed(e -> {
            statusMessage.set("Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        // FIX: Daemon thread để app đóng được
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void translateParagraph(Paragraph p) {
        if (p.getTranslatedText() != null && !p.getTranslatedText().isEmpty()) return;

        statusMessage.set("Translating ID " + p.getId() + "...");
        translationService.translate(p.getOriginalText())
                .thenAccept(translated -> {
                    // FIX: Dùng p.getId() thay vì p.hashCode()
                    storageService.updateParagraphTranslation(p.getId(), translated);
                    Platform.runLater(() -> {
                        p.setTranslatedText(translated);
                        int index = paragraphList.indexOf(p);
                        if (index >= 0) paragraphList.set(index, p);
                        statusMessage.set("Translation saved.");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusMessage.set("Translation failed: " + ex.getMessage()));
                    return null;
                });
    }

    public void loadSentencesFor(Paragraph p) {
        if (p == null) {
            sentenceList.clear();
            return;
        }

        Task<List<Sentence>> task = new Task<>() {
            @Override
            protected List<Sentence> call() throws Exception {
                // FIX: Dùng p.getId()
                List<Sentence> dbSentences = storageService.getSentencesByParagraphId(p.getId());

                if (!dbSentences.isEmpty()) {
                    return dbSentences;
                }

                List<Sentence> newSentences = new ArrayList<>();
                BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
                String text = p.getOriginalText();
                iterator.setText(text);
                int start = iterator.first();
                for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                    String sentenceText = text.substring(start, end).trim();
                    if (!sentenceText.isEmpty()) {
                        // FIX: Liên kết với p.getId()
                        newSentences.add(new Sentence(p.getId(), sentenceText, null));
                    }
                }

                if (!newSentences.isEmpty()) {
                    storageService.saveSentences(newSentences);
                    return storageService.getSentencesByParagraphId(p.getId());
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> sentenceList.setAll(task.getValue()));

        // FIX: Daemon thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void analyzeSentence(Sentence s) {
        if (s.getAnalysis() != null && !s.getAnalysis().isEmpty()) return;

        statusMessage.set("Analyzing grammar...");
        translationService.analyze(s.getOriginal())
                .thenAccept(result -> {
                    storageService.updateSentenceAnalysis(s.getId(), result);
                    Platform.runLater(() -> {
                        s.setAnalysis(result);
                        int index = sentenceList.indexOf(s);
                        if (index >= 0) sentenceList.set(index, s);
                        statusMessage.set("Analysis saved.");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusMessage.set("Analysis error: " + ex.getMessage()));
                    return null;
                });
    }

    // Getter cho App dùng để đóng DB
    public IStorageService getStorageService() {
        return storageService;
    }

    public ObservableList<Paragraph> getParagraphList() { return paragraphList.get(); }
    public ListProperty<Paragraph> paragraphListProperty() { return paragraphList; }
    public ObservableList<Image> getPdfPages() { return pdfPages.get(); }
    public ListProperty<Image> pdfPagesProperty() { return pdfPages; }
    public ObservableList<Sentence> getSentenceList() { return sentenceList.get(); }
    public ListProperty<Sentence> sentenceListProperty() { return sentenceList; }
    public ObjectProperty<Paragraph> selectedParagraphProperty() { return selectedParagraph; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public IntegerProperty appFontSizeProperty() { return appFontSize; }
    public void setAppFontSize(int size) { this.appFontSize.set(size); }
}