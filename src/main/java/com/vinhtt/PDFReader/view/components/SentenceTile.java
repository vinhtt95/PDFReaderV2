package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Sentence;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane; // Import mới
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.util.function.Consumer;

public class SentenceTile extends ListCell<Sentence> {
    private final VBox root = new VBox(8);
    private final Button analyzeBtn = new Button("Analyze Grammar");
    private final Button vocabBtn = new Button("Dictionary");
    private final HBox footer = new HBox(10);
    private final Consumer<Sentence> onAnalyzeAction;

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

    private String renderMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlContent = renderer.render(parser.parse(markdown));

        // Inject CSS để match với Dark Theme
        return "<html><head><style>" +
                "body { background-color: #161b22; color: #c9d1d9; font-family: 'Segoe UI', sans-serif; font-size: 14px; margin: 0; padding: 0; }" +
                "strong { color: #58a6ff; }" +
                "code { background-color: #30363d; padding: 2px 4px; border-radius: 4px; font-family: monospace; }" +
                "ul { padding-left: 20px; }" +
                "p { margin-top: 5px; margin-bottom: 5px; }" +
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

            // 1. Sentence
            root.getChildren().add(createCollapsibleSection("Sentence", item.getOriginal(), false, true));

            // 2. Analysis
            if (item.getAnalysis() != null && !item.getAnalysis().isEmpty()) {
                root.getChildren().add(createCollapsibleSection("Grammar Analysis", item.getAnalysis(), true, true));
            } else {
                // Toolbar
                footer.getChildren().clear();
                analyzeBtn.setText("Analyze Grammar");
                analyzeBtn.setDisable(false);
                footer.getChildren().addAll(analyzeBtn, vocabBtn);
                root.getChildren().add(footer);
            }

            setGraphic(root);
        }
    }

    private VBox createCollapsibleSection(String title, String text, boolean isMarkdown, boolean isExpanded) {
        VBox section = new VBox(2);
        Label header = new Label((isExpanded ? "▼ " : "▶ ") + title);
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);

        Region content;

        if (isMarkdown) {
            WebView webView = new WebView();
            webView.getEngine().loadContent(renderMarkdownToHtml(text));
            webView.setPageFill(javafx.scene.paint.Color.TRANSPARENT);
            webView.setContextMenuEnabled(false);
            webView.setPrefHeight(300);

            // FIX: Bọc WebView trong StackPane (StackPane là một Region)
            StackPane wrapper = new StackPane(webView);
            content = wrapper;
        } else {
            Label label = new Label(text);
            label.getStyleClass().add("text-english");
            label.setWrapText(true);
            content = label;
        }

        // Binding chung cho Region (Giờ StackPane sẽ nhận binding này)
        content.prefWidthProperty().bind(root.widthProperty());
        content.setMaxWidth(Region.USE_PREF_SIZE);

        content.setManaged(isExpanded);
        content.setVisible(isExpanded);

        header.setOnMouseClicked(e -> {
            boolean newState = !content.isVisible();
            content.setVisible(newState);
            content.setManaged(newState);
            header.setText((newState ? "▼ " : "▶ ") + title);
        });

        section.getChildren().addAll(header, content);
        return section;
    }
}