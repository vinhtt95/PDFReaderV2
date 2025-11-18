package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
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
    private final Label originalLabel = new Label();
    private final Label translatedLabel = new Label();
    private final Region separator = new Region(); // Dòng kẻ phân cách
    private final Button translateBtn = new Button("Translate Paragraph");
    private final HBox header = new HBox(translateBtn);
    private final Consumer<Paragraph> onTranslateAction;

    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        root.setPadding(new Insets(15));
        root.getStyleClass().add("card-view"); // Dùng class CSS

        root.prefWidthProperty().bind(this.widthProperty().subtract(30));
        root.setMaxWidth(Region.USE_PREF_SIZE);

        // Original Text
        originalLabel.setWrapText(true);
        originalLabel.getStyleClass().add("text-english");
        originalLabel.prefWidthProperty().bind(root.widthProperty());

        // Separator
        separator.getStyleClass().add("separator-line");
        separator.prefWidthProperty().bind(root.widthProperty());

        // Translated Text
        translatedLabel.setWrapText(true);
        translatedLabel.getStyleClass().add("text-vietnamese");
        translatedLabel.prefWidthProperty().bind(root.widthProperty());

        // Button
        translateBtn.getStyleClass().add("button-action");
        translateBtn.setOnAction(e -> {
            if (getItem() != null) {
                translateBtn.setText("Translating...");
                translateBtn.setDisable(true);
                onTranslateAction.accept(getItem());
            }
        });

        HBox.setHgrow(header, Priority.ALWAYS);
        header.setStyle("-fx-alignment: CENTER_RIGHT;");

        root.getChildren().addAll(header, originalLabel, separator, translatedLabel);
    }

    @Override
    protected void updateItem(Paragraph item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            originalLabel.setText(item.getOriginalText());

            if (item.getTranslatedText() != null && !item.getTranslatedText().isEmpty()) {
                translatedLabel.setText(item.getTranslatedText());
                translatedLabel.setVisible(true);
                translatedLabel.setManaged(true);
                separator.setVisible(true);
                separator.setManaged(true);

                translateBtn.setVisible(false);
                translateBtn.setManaged(false);
            } else {
                translatedLabel.setVisible(false);
                translatedLabel.setManaged(false);
                separator.setVisible(false);
                separator.setManaged(false);

                translateBtn.setText("Translate Paragraph");
                translateBtn.setDisable(false);
                translateBtn.setVisible(true);
                translateBtn.setManaged(true);
            }

            setGraphic(root);
        }
    }
}