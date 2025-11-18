package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ParagraphTile extends ListCell<Paragraph> {
    private final VBox root = new VBox(8);
    private final Button translateBtn = new Button("Translate Paragraph");
    private final HBox footer = new HBox(translateBtn);
    private final Consumer<Paragraph> onTranslateAction;

    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        // 1. Main Layout
        root.setPadding(new Insets(15));
        root.getStyleClass().add("card-view");

        // 2. Footer Setup
        translateBtn.getStyleClass().add("button-action");
        translateBtn.setOnAction(e -> {
            if (getItem() != null) {
                translateBtn.setText("Translating...");
                translateBtn.setDisable(true);
                onTranslateAction.accept(getItem());
            }
        });

        footer.setAlignment(Pos.BOTTOM_RIGHT);
        footer.setPadding(new Insets(5, 0, 0, 0));
    }

    @Override
    protected void updateItem(Paragraph item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            // Unbind để tránh leak khi cell rỗng
            root.prefWidthProperty().unbind();
        } else {
            // BINDING CHIỀU RỘNG CHUẨN
            // Trừ đi khoảng 45px (padding trái/phải của ListView + scrollbar)
            if (getListView() != null) {
                root.prefWidthProperty().bind(getListView().widthProperty().subtract(45));
                root.setMaxWidth(Region.USE_PREF_SIZE);
            }

            root.getChildren().clear();

            // 1. Original
            root.getChildren().add(createCollapsibleSection("Original", item.getOriginalText(), "text-english", true));

            // 2. Translated (nếu có)
            if (item.getTranslatedText() != null && !item.getTranslatedText().isEmpty()) {
                root.getChildren().add(createCollapsibleSection("Translation", item.getTranslatedText(), "text-vietnamese", true));
            } else {
                // Nếu chưa dịch -> Hiện nút Translate
                translateBtn.setText("Translate Paragraph");
                translateBtn.setDisable(false);
                root.getChildren().add(footer);
            }

            setGraphic(root);
        }
    }

    private VBox createCollapsibleSection(String title, String text, String cssClass, boolean isExpanded) {
        VBox section = new VBox(4);

        Label header = new Label((isExpanded ? "▼ " : "▶ ") + title);
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);

        Label content = new Label(text);
        content.getStyleClass().add(cssClass);
        content.setWrapText(true);

        // BINDING CONTENT WIDTH THEO ROOT
        // Quan trọng: minHeight phải là USE_PREF_SIZE để wrap text hoạt động đúng
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