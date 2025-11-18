package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Sentence;
import com.vinhtt.PDFReader.util.ConfigLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.util.function.Consumer;

/**
 * ListCell tùy chỉnh để hiển thị chi tiết câu (Sentence).
 * Hỗ trợ hiển thị phân tích ngữ pháp dạng Markdown/HTML.
 */
public class SentenceTile extends ListCell<Sentence> {
    private final VBox root = new VBox(8);
    private final Button analyzeBtn = new Button("Analyze Grammar");
    private final Button vocabBtn = new Button("Dictionary");
    private final HBox footer = new HBox(10);
    private final Consumer<Sentence> onAnalyzeAction;

    private static final double ANALYSIS_HEIGHT = 300.0;

    /**
     * Khởi tạo SentenceTile.
     * @param onAnalyzeAction Callback xử lý khi nhấn nút phân tích
     */
    public SentenceTile(Consumer<Sentence> onAnalyzeAction) {
        this.onAnalyzeAction = onAnalyzeAction;

        root.setPadding(new Insets(10));
        root.getStyleClass().add("card-view");

        analyzeBtn.getStyleClass().add("button-action");
        vocabBtn.getStyleClass().add("button-action");

        analyzeBtn.setOnAction(e -> {
            if(getItem() != null) {
                analyzeBtn.setText("Processing...");
                analyzeBtn.setDisable(true);
                onAnalyzeAction.accept(getItem());
            }
        });

        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(5, 0, 0, 0));
    }

    /**
     * Chuyển đổi nội dung Markdown sang HTML có style.
     * @param markdown Chuỗi markdown đầu vào
     * @return Chuỗi HTML đầy đủ
     */
    private String renderMarkdownToHtml(String markdown) {
        if (markdown == null) return "";

        int fontSize = ConfigLoader.getFontSize();

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlContent = renderer.render(parser.parse(markdown));

        return "<html><head><style>" +
                "body { " +
                "   background-color: #161b22; " +
                "   color: #c9d1d9; " +
                "   font-family: 'Segoe UI', sans-serif; " +
                "   font-size: " + fontSize + "px; " +
                "   line-height: 1.6; " +
                "   margin: 0; " +
                "   padding: 10px; " +
                "   overflow-x: hidden; " +
                "} " +
                "strong { color: #58a6ff; font-weight: bold; } " +
                "code { " +
                "   background-color: #30363d; " +
                "   padding: 2px 5px; " +
                "   border-radius: 4px; " +
                "   font-family: 'Consolas', monospace; " +
                "   color: #ff7b72; " +
                "   font-size: " + (fontSize - 1) + "px; " +
                "} " +
                "ul, ol { padding-left: 20px; margin: 5px 0; } " +
                "li { margin-bottom: 4px; } " +
                "p { margin: 5px 0; } " +
                "</style></head><body>" +
                htmlContent +
                "</body></html>";
    }

    @Override
    protected void updateItem(Sentence item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            root.prefWidthProperty().unbind();
        } else {
            if (getListView() != null) {
                root.prefWidthProperty().bind(getListView().widthProperty().subtract(45));
                root.setMaxWidth(Region.USE_PREF_SIZE);
            }

            root.getChildren().clear();

            root.getChildren().add(createCollapsibleSection("Sentence", item.getOriginal(), false, true, null));

            if (item.getAnalysis() != null && !item.getAnalysis().isEmpty()) {
                Runnable onReAnalyze = () -> onAnalyzeAction.accept(item);
                root.getChildren().add(createCollapsibleSection("Grammar Analysis", item.getAnalysis(), true, true, onReAnalyze));
            } else {
                footer.getChildren().clear();
                analyzeBtn.setText("Analyze Grammar");
                analyzeBtn.setDisable(false);
                footer.getChildren().addAll(analyzeBtn, vocabBtn);
                root.getChildren().add(footer);
            }

            setGraphic(root);
        }
    }

    /**
     * Tạo section hiển thị nội dung, hỗ trợ Markdown WebView hoặc Label thường.
     * @param title Tiêu đề section
     * @param text Nội dung văn bản
     * @param isMarkdown true nếu nội dung là Markdown cần render HTML
     * @param isExpanded Trạng thái mở rộng mặc định
     * @param onRefresh Callback khi nhấn nút refresh (có thể null)
     * @return VBox chứa section
     */
    private VBox createCollapsibleSection(String title, String text, boolean isMarkdown, boolean isExpanded, Runnable onRefresh) {
        VBox section = new VBox(5);

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("section-header");

        Label headerLabel = new Label((isExpanded ? "▼ " : "▶ ") + title);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(headerLabel, Priority.ALWAYS);
        headerBox.getChildren().add(headerLabel);

        if (onRefresh != null) {
            Button refreshBtn = new Button("↻");
            refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-padding: 0 5px; -fx-cursor: hand;");
            refreshBtn.setTooltip(new Tooltip("Re-analyze"));
            refreshBtn.setOnAction(e -> {
                e.consume();
                onRefresh.run();
            });
            headerBox.getChildren().add(refreshBtn);
        }

        Region content;

        if (isMarkdown) {
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.setPageFill(javafx.scene.paint.Color.TRANSPARENT);
            webView.getEngine().loadContent(renderMarkdownToHtml(text));

            webView.setMinHeight(ANALYSIS_HEIGHT);
            webView.setPrefHeight(ANALYSIS_HEIGHT);
            webView.setMaxHeight(ANALYSIS_HEIGHT);
            webView.setMinWidth(200);

            StackPane wrapper = new StackPane(webView);
            wrapper.setMinHeight(ANALYSIS_HEIGHT);
            wrapper.setPrefHeight(ANALYSIS_HEIGHT);

            wrapper.setStyle("-fx-border-color: #30363d; -fx-border-width: 1px; -fx-border-radius: 4px;");

            webView.prefWidthProperty().bind(wrapper.widthProperty());
            content = wrapper;
        } else {
            Label label = new Label(text);
            label.getStyleClass().add("text-english");
            label.setWrapText(true);

            label.setMaxWidth(Region.USE_PREF_SIZE);
            label.setMinHeight(Region.USE_PREF_SIZE);

            content = label;
        }

        content.prefWidthProperty().bind(root.widthProperty());
        content.setManaged(isExpanded);
        content.setVisible(isExpanded);

        headerLabel.setOnMouseClicked(e -> {
            boolean newState = !content.isVisible();
            content.setVisible(newState);
            content.setManaged(newState);
            headerLabel.setText((newState ? "▼ " : "▶ ") + title);
        });

        section.getChildren().addAll(headerBox, content);
        return section;
    }
}