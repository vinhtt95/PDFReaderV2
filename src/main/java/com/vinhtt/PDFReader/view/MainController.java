package com.vinhtt.PDFReader.view;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
import com.vinhtt.PDFReader.view.components.ParagraphTile;
import com.vinhtt.PDFReader.view.components.SentenceTile; // Import mới
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

public class MainController {

    @FXML private ScrollPane pdfContainer;
    @FXML private VBox pdfPageContainer;
    @FXML private ListView<Paragraph> paragraphListView;
    @FXML private ListView<Sentence> sentenceListView; // Đổi từ TextArea sang ListView
    @FXML private Label statusLabel;

    private final MainViewModel viewModel = new MainViewModel();

    @FXML
    public void initialize() {
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // --- CỘT 2: Paragraph List ---
        paragraphListView.setCellFactory(param -> new ParagraphTile(viewModel::translateParagraph));
        paragraphListView.itemsProperty().bind(viewModel.paragraphListProperty());

        // --- CỘT 3: Sentence List (MỚI) ---
        sentenceListView.setCellFactory(param -> new SentenceTile(viewModel::analyzeSentence));
        sentenceListView.itemsProperty().bind(viewModel.sentenceListProperty());

        // --- Selection Logic ---
        paragraphListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.selectedParagraphProperty().set(newVal);
            // Khi chọn đoạn văn, gọi ViewModel để tách câu
            if (newVal != null) {
                viewModel.loadSentencesFor(newVal);
            }
        });

        // --- PDF Rendering ---
        viewModel.pdfPagesProperty().addListener((ListChangeListener<Image>) c -> {
            pdfPageContainer.getChildren().clear();
            for (Image img : c.getList()) {
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                imageView.fitWidthProperty().bind(pdfContainer.widthProperty().subtract(20));
                pdfPageContainer.getChildren().add(imageView);
            }
        });
    }

    // ... (Giữ nguyên các hàm onOpenPdf, onOpenSettings)
    public void onOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(pdfContainer.getScene().getWindow());
        if (file != null) {
            viewModel.loadPdf(file);
        }
    }

    public void onOpenSettings() {
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