package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon; // Nếu bạn dùng icon, không thì dùng text

import java.util.function.Consumer;

public class ParagraphTile extends ListCell<Paragraph> {
    private final VBox root = new VBox(5);
    private final Button translateBtn = new Button("Translate");
    private final Consumer<Paragraph> onTranslateAction;

    // Containers cho các section để có thể update nội dung sau
    private VBox originalSection;
    private VBox translatedSection;

    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        // 1. Main Layout
        root.setPadding(new Insets(10));
        root.getStyleClass().add("card-view");

        // BINDING QUAN TRỌNG ĐỂ WRAP TEXT:
        // Chiều rộng root = Chiều rộng Cell - Padding (30px)
        root.prefWidthProperty().bind(this.widthProperty().subtract(30));
        root.setMaxWidth(Region.USE_PREF_SIZE);

        // 2. Setup Button
        translateBtn.getStyleClass().add("button-action");
        translateBtn.setMaxWidth(Double.MAX_VALUE);
        translateBtn.setOnAction(e -> {
            if (getItem() != null) {
                translateBtn.setDisable(true);
                translateBtn.setText("Translating...");
                onTranslateAction.accept(getItem());
            }
        });
    }

    @Override
    protected void updateItem(Paragraph item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
        } else {
            root.getChildren().clear();

            // 1. Header & Translate Button
            HBox headerBox = new HBox();
            headerBox.setAlignment(Pos.CENTER_RIGHT);

            if (item.getTranslatedText() == null || item.getTranslatedText().isEmpty()) {
                translateBtn.setText("Translate");
                translateBtn.setDisable(false);
                headerBox.getChildren().add(translateBtn);
            }

            if (!headerBox.getChildren().isEmpty()) {
                root.getChildren().add(headerBox);
            }

            // 2. Original Text Section (Luôn hiện, Default Expanded)
            originalSection = createCollapsibleSection("Original", item.getOriginalText(), "text-english", true);
            root.getChildren().add(originalSection);

            // 3. Translated Text Section (Nếu có)
            if (item.getTranslatedText() != null && !item.getTranslatedText().isEmpty()) {
                translatedSection = createCollapsibleSection("Translation", item.getTranslatedText(), "text-vietnamese", true);
                root.getChildren().add(translatedSection);
            }

            setGraphic(root);
        }
    }

    /**
     * Helper tạo một section có thể collapse/expand
     */
    private VBox createCollapsibleSection(String title, String text, String cssClass, boolean isExpanded) {
        VBox section = new VBox(2);

        // Header (Clickable)
        Label header = new Label((isExpanded ? "▼ " : "▶ ") + title);
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);

        // Content
        Label content = new Label(text);
        content.getStyleClass().add(cssClass);
        content.setWrapText(true);

        // BINDING QUAN TRỌNG: Ép Label không được vượt quá chiều rộng của section cha
        content.maxWidthProperty().bind(root.widthProperty().subtract(20)); // Trừ thêm padding nội bộ
        content.setMinHeight(Region.USE_PREF_SIZE);

        // State control
        content.setManaged(isExpanded);
        content.setVisible(isExpanded);

        // Click Event
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