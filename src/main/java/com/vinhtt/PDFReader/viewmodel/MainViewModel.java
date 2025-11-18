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
import java.util.stream.Collectors;

/**
 * ViewModel chính quản lý trạng thái và logic nghiệp vụ của màn hình đọc PDF.
 */
public class MainViewModel {

    private final List<Paragraph> allParagraphs = new ArrayList<>();
    private final List<Image> allPdfImages = new ArrayList<>();

    private final ListProperty<Paragraph> visibleParagraphList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Image> currentPdfPageImage = new SimpleObjectProperty<>();

    private final ListProperty<Sentence> sentenceList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Paragraph> selectedParagraph = new SimpleObjectProperty<>();

    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final IntegerProperty appFontSize = new SimpleIntegerProperty(ConfigLoader.getFontSize());

    private final IPdfService pdfService;
    private final ITranslationService translationService;
    private final IStorageService storageService;

    /**
     * Khởi tạo ViewModel và các service đi kèm.
     */
    public MainViewModel() {
        this.pdfService = new PdfBoxService();
        this.translationService = new GeminiService();
        this.storageService = new SqliteStorageService();

        currentPage.addListener((obs, oldVal, newVal) -> updateCurrentPageView());
    }

    /**
     * Cập nhật dữ liệu hiển thị khi người dùng chuyển trang.
     */
    private void updateCurrentPageView() {
        int page = currentPage.get();
        if (page >= 0 && page < allPdfImages.size()) {
            currentPdfPageImage.set(allPdfImages.get(page));

            List<Paragraph> pageParagraphs = allParagraphs.stream()
                    .filter(p -> p.getPageIndex() == page)
                    .collect(Collectors.toList());
            visibleParagraphList.setAll(pageParagraphs);

            selectedParagraph.set(null);
            sentenceList.clear();
        }
    }

    /**
     * Chuyển sang trang kế tiếp.
     */
    public void nextPage() {
        if (currentPage.get() < totalPages.get() - 1) {
            currentPage.set(currentPage.get() + 1);
        }
    }

    /**
     * Quay lại trang trước đó.
     */
    public void prevPage() {
        if (currentPage.get() > 0) {
            currentPage.set(currentPage.get() - 1);
        }
    }

    /**
     * Nhảy tới một trang cụ thể.
     * @param pageIndex chỉ số trang (bắt đầu từ 0)
     */
    public void goToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < totalPages.get()) {
            currentPage.set(pageIndex);
        }
    }

    /**
     * Tải file PDF, render hình ảnh và parse text.
     * @param file file PDF đầu vào
     */
    public void loadPdf(File file) {
        statusMessage.set("Initializing...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String dbPath = file.getParent() + File.separator + file.getName() + ".meta.db";
                storageService.initDatabase(dbPath);

                updateMessage("Rendering PDF Pages...");
                List<BufferedImage> bufferedImages = pdfService.renderPdfPages(file);
                allPdfImages.clear();
                for (BufferedImage bi : bufferedImages) {
                    allPdfImages.add(SwingFXUtils.toFXImage(bi, null));
                }

                if (storageService.hasData()) {
                    updateMessage("Loading Data...");
                    allParagraphs.clear();
                    allParagraphs.addAll(storageService.getAllParagraphs());
                } else {
                    updateMessage("Parsing PDF Text...");
                    List<Paragraph> parsed = pdfService.parsePdf(file);
                    storageService.saveParagraphs(parsed);
                    allParagraphs.clear();
                    allParagraphs.addAll(parsed);
                }

                Platform.runLater(() -> {
                    totalPages.set(allPdfImages.size());
                    currentPage.set(0);
                    updateCurrentPageView();
                    statusMessage.set("Loaded: " + file.getName());
                });
                return null;
            }
        };

        task.messageProperty().addListener((obs, old, msg) -> statusMessage.set(msg));
        task.setOnFailed(e -> {
            statusMessage.set("Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Dịch đoạn văn bản được chọn.
     * Hàm này cho phép gọi lại để dịch lại (Re-translate) đè lên nội dung cũ.
     * @param p Đoạn văn cần dịch
     */
    public void translateParagraph(Paragraph p) {
        statusMessage.set("Translating...");
        translationService.translate(p.getOriginalText())
                .thenAccept(translated -> {
                    storageService.updateParagraphTranslation(p.getId(), translated);
                    Platform.runLater(() -> {
                        p.setTranslatedText(translated);
                        int index = visibleParagraphList.indexOf(p);
                        if (index >= 0) visibleParagraphList.set(index, p);
                        statusMessage.set("Done.");
                    });
                });
    }

    /**
     * Tách câu cho đoạn văn được chọn và load dữ liệu từ DB nếu có.
     * @param p Đoạn văn được chọn
     */
    public void loadSentencesFor(Paragraph p) {
        if (p == null) {
            sentenceList.clear();
            return;
        }

        Task<List<Sentence>> task = new Task<>() {
            @Override
            protected List<Sentence> call() throws Exception {
                List<Sentence> dbSentences = storageService.getSentencesByParagraphId(p.getId());
                if (!dbSentences.isEmpty()) return dbSentences;

                List<Sentence> newSentences = new ArrayList<>();
                BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
                String text = p.getOriginalText();
                iterator.setText(text);
                int start = iterator.first();
                for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                    String s = text.substring(start, end).trim();
                    if (!s.isEmpty()) newSentences.add(new Sentence(p.getId(), s, null));
                }
                if (!newSentences.isEmpty()) storageService.saveSentences(newSentences);
                return storageService.getSentencesByParagraphId(p.getId());
            }
        };
        task.setOnSucceeded(e -> sentenceList.setAll(task.getValue()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Phân tích ngữ pháp cho câu được chọn.
     * Hàm này cho phép gọi lại để phân tích lại (Re-analyze) đè lên nội dung cũ.
     * @param s Câu cần phân tích
     */
    public void analyzeSentence(Sentence s) {
        statusMessage.set("Analyzing...");
        translationService.analyze(s.getOriginal())
                .thenAccept(result -> {
                    storageService.updateSentenceAnalysis(s.getId(), result);
                    Platform.runLater(() -> {
                        s.setAnalysis(result);
                        int index = sentenceList.indexOf(s);
                        if (index >= 0) sentenceList.set(index, s);
                        statusMessage.set("Done.");
                    });
                });
    }

    public IStorageService getStorageService() { return storageService; }

    public ObservableList<Paragraph> getVisibleParagraphList() { return visibleParagraphList.get(); }
    public ListProperty<Paragraph> visibleParagraphListProperty() { return visibleParagraphList; }

    public ObjectProperty<Image> currentPdfPageImageProperty() { return currentPdfPageImage; }

    public IntegerProperty currentPageProperty() { return currentPage; }
    public IntegerProperty totalPagesProperty() { return totalPages; }

    public ObservableList<Sentence> getSentenceList() { return sentenceList.get(); }
    public ListProperty<Sentence> sentenceListProperty() { return sentenceList; }
    public ObjectProperty<Paragraph> selectedParagraphProperty() { return selectedParagraph; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public IntegerProperty appFontSizeProperty() { return appFontSize; }
    public void setAppFontSize(int size) { this.appFontSize.set(size); }
}