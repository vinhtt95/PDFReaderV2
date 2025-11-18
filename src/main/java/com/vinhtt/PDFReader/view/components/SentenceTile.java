package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Sentence;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SentenceTile extends ListCell<Sentence> {
    private final VBox root = new VBox(5);
    private final Button analyzeBtn = new Button("Analyze Grammar");
    private final Button vocabBtn = new Button("Dictionary Lookup");
    private final Consumer<Sentence> onAnalyzeAction;

    public SentenceTile(Consumer<Sentence> onAnalyzeAction) {
        this.onAnalyzeAction = onAnalyzeAction;

        root.setPadding(new Insets(10));
        root.getStyleClass().add("card-view");

        // Fix Wrap Text
        root.prefWidthProperty().bind(this.widthProperty().subtract(30));
        root.setMaxWidth(Region.USE_PREF_SIZE);

        // Buttons style
        analyzeBtn.getStyleClass().add("button-action");
        vocabBtn.getStyleClass().add("button-action");

        analyzeBtn.setOnAction(e -> {
            if(getItem() != null) {
                analyzeBtn.setText("Processing...");
                analyzeBtn.setDisable(true);
                onAnalyzeAction.accept(getItem());
            }
        });
        // TODO: Implement vocabBtn action
    }

    @Override
    protected void updateItem(Sentence item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            root.getChildren().clear();

            // 1. Toolbar
            FlowPane toolbar = new FlowPane(10, 10);
            if (item.getAnalysis() == null || item.getAnalysis().isEmpty()) {
                analyzeBtn.setText("Analyze Grammar");
                analyzeBtn.setDisable(false);
                toolbar.getChildren().add(analyzeBtn);
            }
            // toolbar.getChildren().add(vocabBtn); // Enable when ready

            if (!toolbar.getChildren().isEmpty()) {
                root.getChildren().add(toolbar);
            }

            // 2. Original Sentence (Expand by default)
            root.getChildren().add(createCollapsibleSection("Sentence", item.getOriginal(), "text-english", true));

            // 3. Analysis (If exists)
            if (item.getAnalysis() != null && !item.getAnalysis().isEmpty()) {
                root.getChildren().add(createCollapsibleSection("Grammar Analysis", item.getAnalysis(), "text-vietnamese", true));
            }

            setGraphic(root);
        }
    }

    private VBox createCollapsibleSection(String title, String text, String cssClass, boolean isExpanded) {
        VBox section = new VBox(2);
        Label header = new Label((isExpanded ? "▼ " : "▶ ") + title);
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);

        Label content = new Label(text);
        content.getStyleClass().add(cssClass);
        content.setWrapText(true);
        content.maxWidthProperty().bind(root.widthProperty().subtract(20));
        content.setMinHeight(Region.USE_PREF_SIZE);

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