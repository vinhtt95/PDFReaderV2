package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Sentence;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SentenceTile extends ListCell<Sentence> {
    private final VBox root = new VBox(8);
    private final Label sentenceLabel = new Label();
    private final Label analysisLabel = new Label();
    private final Button analyzeBtn = new Button("Analyze Grammar");
    private final Button vocabBtn = new Button("Dictionary");
    private final HBox toolBar = new HBox(10);

    private final Consumer<Sentence> onAnalyzeAction;

    public SentenceTile(Consumer<Sentence> onAnalyzeAction) {
        this.onAnalyzeAction = onAnalyzeAction;

        // Layout
        root.setPadding(new Insets(10));
        root.getStyleClass().add("card-view"); // CSS class

        // Binding width
        root.prefWidthProperty().bind(this.widthProperty().subtract(20));
        root.setMaxWidth(Region.USE_PREF_SIZE);

        // Sentence Text
        sentenceLabel.setWrapText(true);
        sentenceLabel.getStyleClass().add("text-english");
        sentenceLabel.prefWidthProperty().bind(root.widthProperty());

        // Analysis Result Text
        analysisLabel.setWrapText(true);
        analysisLabel.getStyleClass().add("text-vietnamese"); // Reuse style for secondary text
        analysisLabel.setStyle("-fx-text-fill: #58a6ff;"); // Override color for analysis
        analysisLabel.prefWidthProperty().bind(root.widthProperty());
        analysisLabel.setManaged(false);
        analysisLabel.setVisible(false);

        // Buttons
        analyzeBtn.getStyleClass().add("button-action");
        vocabBtn.getStyleClass().add("button-action");

        analyzeBtn.setOnAction(e -> {
            if (getItem() != null) {
                analyzeBtn.setDisable(true);
                analyzeBtn.setText("Analyzing...");
                onAnalyzeAction.accept(getItem());
            }
        });

        toolBar.getChildren().addAll(analyzeBtn, vocabBtn);
        root.getChildren().addAll(sentenceLabel, analysisLabel, toolBar);
    }

    @Override
    protected void updateItem(Sentence item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            sentenceLabel.setText(item.getOriginal());

            // Hiển thị kết quả phân tích nếu có
            if (item.getAnalysis() != null && !item.getAnalysis().isEmpty()) {
                analysisLabel.setText("Analysis: " + item.getAnalysis());
                analysisLabel.setVisible(true);
                analysisLabel.setManaged(true);
                analyzeBtn.setVisible(false); // Ẩn nút analyze khi đã có kết quả
            } else {
                analysisLabel.setVisible(false);
                analysisLabel.setManaged(false);
                analyzeBtn.setVisible(true);
                analyzeBtn.setDisable(false);
                analyzeBtn.setText("Analyze Grammar");
            }

            setGraphic(root);
        }
    }
}