package com.vinhtt.PDFReader.view.components;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ParagraphTile extends ListCell<Paragraph> {
    private final VBox root = new VBox(5);
    private final Label textLabel = new Label();
    private final Button translateBtn = new Button("Translate");
    private final HBox header = new HBox(translateBtn);
    private final Consumer<Paragraph> onTranslateAction;

    public ParagraphTile(Consumer<Paragraph> onTranslateAction) {
        this.onTranslateAction = onTranslateAction;

        root.setPadding(new Insets(10));
        root.getStyleClass().add("paragraph-card"); // Define in CSS

        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("text-english");

        // Styling button
        translateBtn.getStyleClass().add("accent");
        translateBtn.setOnAction(e -> {
            if (getItem() != null) {
                onTranslateAction.accept(getItem());
            }
        });

        root.getChildren().addAll(header, textLabel);
    }

    @Override
    protected void updateItem(Paragraph item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            if (item.getTranslatedText() != null) {
                textLabel.setText(item.getTranslatedText());
                textLabel.setStyle("-fx-text-fill: #8b949e;"); // Change color if translated
                translateBtn.setVisible(false);
            } else {
                textLabel.setText(item.getOriginalText());
                textLabel.setStyle("-fx-text-fill: #c9d1d9;");
                translateBtn.setVisible(true);
            }
            setGraphic(root);
        }
    }
}