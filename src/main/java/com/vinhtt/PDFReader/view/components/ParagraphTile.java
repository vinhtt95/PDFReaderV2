package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ParagraphTile extends ListCell<Paragraph> {
    private final VBox root = new VBox(8); // Tăng khoảng cách spacer một chút
    private final Button translateBtn = new Button("Translate Paragraph");
    private final HBox footer = new HBox(translateBtn); // Footer chứa nút
    private final Consumer<Paragraph> onTranslateAction;

    // Containers cho các section
    private VBox originalSection;
    private VBox translatedSection;

    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        // 1. Main Layout
        root.setPadding(new Insets(15));
        root.getStyleClass().add("card-view");

        // Lưu ý: Không bind width ở constructor nữa để tránh lỗi chiều cao

        // 2. Setup Footer (Nút ở góc dưới phải)
        translateBtn.getStyleClass().add("button-action");
        translateBtn.setOnAction(e -> {
            if (getItem() != null) {
                translateBtn.setText("Translating...");
                translateBtn.setDisable(true);
                onTranslateAction.accept(getItem());
            }
        });

        footer.setAlignment(Pos.BOTTOM_RIGHT); // Căn phải
        footer.setPadding(new Insets(5, 0, 0, 0)); // Cách biệt với nội dung trên
    }

    @Override
    protected void updateItem(Paragraph item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            // Gỡ bind để tránh leak memory
            root.prefWidthProperty().unbind();
        } else {
            // FIX LỖI CHIỀU CAO LỚN: Bind trực tiếp vào ListView cha
            if (getListView() != null) {
                // Trừ 40px: 15px padding trái + 15px padding phải + 10px scrollbar dư
                root.prefWidthProperty().bind(getListView().widthProperty().subtract(40));
                root.setMaxWidth(Region.USE_PREF_SIZE);
            }

            root.getChildren().clear();

            // 1. Original Text Section
            originalSection = createCollapsibleSection("Original", item.getOriginalText(), "text-english", true);
            root.getChildren().add(originalSection);

            // 2. Translated Text Section (Nếu có)
            if (item.getTranslatedText() != null && !item.getTranslatedText().isEmpty()) {
                translatedSection = createCollapsibleSection("Translation", item.getTranslatedText(), "text-vietnamese", true);
                root.getChildren().add(translatedSection);

                // Đã dịch xong -> Ẩn nút Translate (hoặc đổi thành Re-translate tuỳ logic)
                // Ở đây tôi ẩn đi để giao diện sạch
            } else {
                // Chưa dịch -> Hiện nút Translate ở dưới cùng
                translateBtn.setText("Translate Paragraph");
                translateBtn.setDisable(false);
                root.getChildren().add(footer);
            }

            setGraphic(root);
        }
    }

    private VBox createCollapsibleSection(String title, String text, String cssClass, boolean isExpanded) {
        VBox section = new VBox(4);

        // Header
        Label header = new Label((isExpanded ? "▼ " : "▶ ") + title);
        header.getStyleClass().add("section-header");
        header.setMaxWidth(Double.MAX_VALUE);

        // Content
        Label content = new Label(text);
        content.getStyleClass().add(cssClass);
        content.setWrapText(true);

        // Bind width của Label theo Root để wrap text đúng
        content.prefWidthProperty().bind(root.widthProperty());
        content.setMaxWidth(Region.USE_PREF_SIZE);
        content.setMinHeight(Region.USE_PREF_SIZE); // Bắt buộc để tính toán chiều cao

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