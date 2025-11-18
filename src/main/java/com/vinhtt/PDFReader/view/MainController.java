package com.vinhtt.PDFReader.view;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.view.components.ParagraphTile;
import com.vinhtt.PDFReader.viewmodel.MainViewModel;
import com.vinhtt.PDFReader.util.ConfigLoader;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.collections.ListChangeListener;

import java.io.File;
import java.util.Optional;

public class MainController {

    @FXML private ScrollPane pdfContainer;
    @FXML private VBox pdfPageContainer; // Liên kết với FXML mới
    @FXML private ListView<Paragraph> paragraphListView;
    @FXML private TextArea analysisArea;
    @FXML private Label statusLabel;

    private final MainViewModel viewModel = new MainViewModel();

    @FXML
    public void initialize() {
        // 1. Bind Status
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // 2. Setup Paragraph List
        paragraphListView.setCellFactory(param -> new ParagraphTile(viewModel::translateParagraph));
        paragraphListView.itemsProperty().bind(viewModel.paragraphListProperty());

        // 3. Selection Listener
        paragraphListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.selectedParagraphProperty().set(newVal);
            if (newVal != null) {
                analysisArea.setText("Original: " + newVal.getOriginalText());
            }
        });

        // 4. PDF Image Rendering Listener
        viewModel.pdfPagesProperty().addListener((ListChangeListener<Image>) c -> {
            pdfPageContainer.getChildren().clear();
            for (Image img : c.getList()) {
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                // Bind width để ảnh tự co giãn theo ScrollPane
                imageView.fitWidthProperty().bind(pdfContainer.widthProperty().subtract(20));
                pdfPageContainer.getChildren().add(imageView);
            }
        });
    }

    public void onOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(pdfContainer.getScene().getWindow());
        if (file != null) {
            viewModel.loadPdf(file);
        }
    }

    public void onOpenSettings() {
        // Tạo Dialog custom cho nhiều setting
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Configuration");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField apiKeyField = new TextField(ConfigLoader.getApiKey());
        apiKeyField.setPromptText("Gemini API Key");

        TextField modelField = new TextField(ConfigLoader.getGeminiModel());
        modelField.setPromptText("e.g., gemini-1.5-flash");

        TextArea promptArea = new TextArea(ConfigLoader.getTranslationPrompt());
        promptArea.setPromptText("Custom Prompt...");
        promptArea.setPrefRowCount(3);
        promptArea.setWrapText(true);

        grid.add(new Label("API Key:"), 0, 0);
        grid.add(apiKeyField, 1, 0);
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        grid.add(new Label("Prompt:"), 0, 2);
        grid.add(promptArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Xử lý kết quả
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                ConfigLoader.saveSettings(
                        apiKeyField.getText(),
                        modelField.getText(),
                        promptArea.getText()
                );
                return saveButtonType;
            }
            return null;
        });

        dialog.showAndWait();
    }
}