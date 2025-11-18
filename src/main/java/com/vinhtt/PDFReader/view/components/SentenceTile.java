package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Sentence;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SentenceTile extends ListCell<Sentence> {
    private final VBox root = new VBox(8);
    private final Button analyzeBtn = new Button("Analyze Grammar");
    private final Button vocabBtn = new Button("Dictionary");
    private final HBox footer = new HBox(10); // Footer chứa các nút
    private final Consumer<Sentence> onAnalyzeAction;

    public SentenceTile(Consumer<Sentence> onAnalyzeAction) {
        this.onAnalyzeAction = onAnalyzeAction;

        // 1. Layout
        root.setPadding(new Insets(10));
        root.getStyleClass().add("card-view");

        // 2. Buttons Styling
        analyzeBtn.getStyleClass().add("button-action");
        vocabBtn.getStyleClass().add("button-action");

        analyzeBtn.setOnAction(e -> {
            if(getItem() != null) {
                analyzeBtn.setText("Processing...");
                analyzeBtn.setDisable(true);
                onAnalyzeAction.accept(getItem());
            }
        });

        // 3. Setup Footer (Căn góc dưới phải)
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(5, 0, 0, 0));
    }

    @Override
    protected void updateItem(Sentence item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            root.prefWidthProperty().unbind();
        } else {
            // FIX LỖI CHIỀU CAO: Bind vào ListView cha
            if (getListView() != null) {
                root.prefWidthProperty().bind(getListView().widthProperty().subtract(35));
                root.setMaxWidth(Region.USE_PREF_SIZE);
            }

            root.getChildren().clear();

            // 1. Original Sentence
            root.getChildren().add(createCollapsibleSection("Sentence", item.getOriginal(), "text-english", true));

            // 2. Analysis Result
            if (item.getAnalysis() != null && !item.getAnalysis().isEmpty()) {
                root.getChildren().add(createCollapsibleSection("Grammar Analysis", item.getAnalysis(), "text-vietnamese", true));
                // Đã phân tích xong -> Có thể ẩn nút hoặc hiện kết quả khác
            } else {
                // Chưa phân tích -> Hiện nút ở Footer
                footer.getChildren().clear();
                analyzeBtn.setText("Analyze Grammar");
                analyzeBtn.setDisable(false);

                footer.getChildren().addAll(analyzeBtn, vocabBtn);
                root.getChildren().add(footer);
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

        // Bind width
        content.prefWidthProperty().bind(root.widthProperty());
        content.setMaxWidth(Region.USE_PREF_SIZE);
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