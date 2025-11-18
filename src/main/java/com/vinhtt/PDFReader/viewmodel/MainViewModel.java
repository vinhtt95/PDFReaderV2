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
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ViewModel chính quản lý trạng thái và logic nghiệp vụ của màn hình đọc PDF.
 */
public class MainViewModel {

    private final List<Paragraph> allParagraphs = new ArrayList<>();
    // Thay vì List<Image>, ta giữ PDDocument để render theo yêu cầu
    private PDDocument currentDocument;

    // Cache hình ảnh (LRU Cache đơn giản: giữ lại 5 trang gần nhất để lướt cho mượt)
    private final Map<Integer, Image> pageCache = new ConcurrentHashMap<>();

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

    // Cấu hình Render chất lượng cao
    private static final float HIGH_QUALITY_SCALE = 3.0f; // Tương đương ~216 DPI

    /**
     * Khởi tạo ViewModel và các service đi kèm.
     */
    public MainViewModel() {
        this.pdfService = new PdfBoxService();
        this.translationService = new GeminiService();
        this.storageService = new SqliteStorageService();

        currentPage.addListener((obs, oldVal, newVal) -> {
            updateCurrentPageView();
            // Pre-load trang tiếp theo để trải nghiệm mượt hơn
            preloadPage(newVal.intValue() + 1);
        });
    }

    private void updateCurrentPageView() {
        int page = currentPage.get();
        if (currentDocument == null || page < 0 || page >= totalPages.get()) return;

        // 1. Cập nhật Paragraph List (Text) ngay lập tức
        List<Paragraph> pageParagraphs = allParagraphs.stream()
                .filter(p -> p.getPageIndex() == page)
                .collect(Collectors.toList());
        visibleParagraphList.setAll(pageParagraphs);
        selectedParagraph.set(null);
        sentenceList.clear();

        // 2. Cập nhật Image (Async)
        if (pageCache.containsKey(page)) {
            currentPdfPageImage.set(pageCache.get(page));
        } else {
            // Nếu chưa có trong cache, hiển thị loading hoặc giữ ảnh cũ và chạy task render
            statusMessage.set("Rendering HQ Page " + (page + 1) + "...");
            renderPageAsync(page);
        }
    }

    private void renderPageAsync(int pageIndex) {
        Task<Image> renderTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // Render High Quality
                BufferedImage bi = pdfService.renderPage(currentDocument, pageIndex, HIGH_QUALITY_SCALE);
                return SwingFXUtils.toFXImage(bi, null);
            }
        };

        renderTask.setOnSucceeded(e -> {
            Image img = renderTask.getValue();
            pageCache.put(pageIndex, img);
            // Xóa cache cũ nếu quá lớn (giữ 5 trang)
            if (pageCache.size() > 5) {
                int furthestPage = pageCache.keySet().stream()
                        .max(Comparator.comparingInt(p -> Math.abs(p - pageIndex)))
                        .orElse(pageIndex);
                if (furthestPage != pageIndex) pageCache.remove(furthestPage);
            }

            // Chỉ update nếu user vẫn đang ở trang đó
            if (currentPage.get() == pageIndex) {
                currentPdfPageImage.set(img);
                statusMessage.set("Ready");
            }
        });

        renderTask.setOnFailed(e -> {
            statusMessage.set("Render Error: " + renderTask.getException().getMessage());
            renderTask.getException().printStackTrace();
        });

        new Thread(renderTask).start();
    }

    private void preloadPage(int pageIndex) {
        if (pageIndex < totalPages.get() && !pageCache.containsKey(pageIndex)) {
            renderPageAsync(pageIndex); // Chạy ngầm
        }
    }

    // --- Load PDF Logic ---
    public void loadPdf(File file) {
        // Đóng document cũ nếu có
        closeCurrentDocument();

        statusMessage.set("Initializing...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String dbPath = file.getParent() + File.separator + file.getName() + ".meta.db";
                storageService.initDatabase(dbPath);

                // 1. Load PDF Document (Giữ handle file)
                updateMessage("Opening PDF...");
                currentDocument = pdfService.loadDocument(file);

                Platform.runLater(() -> {
                    totalPages.set(currentDocument.getNumberOfPages());
                    pageCache.clear();
                });

                // 2. Parse Text / Load from DB
                if (storageService.hasData()) {
                    updateMessage("Loading Data...");
                    allParagraphs.clear();
                    allParagraphs.addAll(storageService.getAllParagraphs());
                } else {
                    updateMessage("Parsing Text...");
                    List<Paragraph> parsed = pdfService.parsePdf(file);
                    storageService.saveParagraphs(parsed);
                    allParagraphs.clear();
                    allParagraphs.addAll(parsed);
                }

                // 3. Render trang đầu tiên
                Platform.runLater(() -> {
                    currentPage.set(0);
                    updateCurrentPageView();
                    statusMessage.set("Loaded: " + file.getName());
                });
                return null;
            }
        };

        task.messageProperty().addListener((obs, old, msg) -> statusMessage.set(msg));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void closeCurrentDocument() {
        if (currentDocument != null) {
            try {
                currentDocument.close();
                currentDocument = null;
                pageCache.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
     * Dịch đoạn văn bản được chọn.
     * Hàm này cho phép gọi lại để dịch lại (Re-translate) đè lên nội dung cũ.
     * @param p Đoạn văn cần dịch
     */
    public void translateParagraph(Paragraph p) {
        if (p.getTranslatedText() != null && !p.getTranslatedText().isEmpty()) return;
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