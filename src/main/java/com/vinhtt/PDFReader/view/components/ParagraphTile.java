package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * ListCell tùy chỉnh để hiển thị đoạn văn (Paragraph) trong danh sách.
 * Hỗ trợ hiển thị văn bản gốc, văn bản dịch và nút chức năng.
 */
public class ParagraphTile extends ListCell<Paragraph> {
    private final VBox root = new VBox(8);
    private final Button translateBtn = new Button("Translate Paragraph");
    private final HBox footer = new HBox(translateBtn);
    private final Consumer<Paragraph> onTranslateAction;

    /**
     * Khởi tạo ParagraphTile.
     * @param onTranslateAction Callback xử lý khi nhấn nút dịch
     */
    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        root.setPadding(new Insets(15));
        root.getStyleClass().add("card-view");

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
            root.prefWidthProperty().unbind();
        } else {
            if (getListView() != null) {
                root.prefWidthProperty().bind(getListView().widthProperty().subtract(45));
                root.setMaxWidth(Region.USE_PREF_SIZE);
            }

            root.getChildren().clear();

            root.getChildren().add(createCollapsibleSection("Original", item.getOriginalText(), "text-english", true, null));

            if (item.getTranslatedText() != null && !item.getTranslatedText().isEmpty()) {
                Runnable onReTranslate = () -> onTranslateAction.accept(item);
                root.getChildren().add(createCollapsibleSection("Translation", item.getTranslatedText(), "text-vietnamese", true, onReTranslate));
            } else {
                translateBtn.setText("Translate Paragraph");
                translateBtn.setDisable(false);
                root.getChildren().add(footer);
            }

            setGraphic(root);
        }
    }

    /**
     * Tạo một section có thể thu gọn/mở rộng với tiêu đề và nội dung.
     * @param title Tiêu đề section
     * @param text Nội dung văn bản
     * @param cssClass CSS class cho nội dung
     * @param isExpanded Trạng thái mở rộng mặc định
     * @param onRefresh Callback khi nhấn nút refresh (có thể null)
     * @return VBox chứa section hoàn chỉnh
     */
    private VBox createCollapsibleSection(String title, String text, String cssClass, boolean isExpanded, Runnable onRefresh) {
        VBox section = new VBox(4);

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
            refreshBtn.setTooltip(new Tooltip("Re-translate"));
            refreshBtn.setOnAction(e -> {
                e.consume();
                onRefresh.run();
            });
            headerBox.getChildren().add(refreshBtn);
        }

        Label content = new Label(text);
        content.getStyleClass().add(cssClass);
        content.setWrapText(true);

        content.prefWidthProperty().bind(root.widthProperty());
        content.setMaxWidth(Region.USE_PREF_SIZE);
        content.setMinHeight(Region.USE_PREF_SIZE);

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