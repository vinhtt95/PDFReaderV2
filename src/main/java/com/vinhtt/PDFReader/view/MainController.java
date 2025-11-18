package com.vinhtt.PDFReader.view;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
import com.vinhtt.PDFReader.view.components.ParagraphTile;
import com.vinhtt.PDFReader.view.components.SentenceTile;
import com.vinhtt.PDFReader.viewmodel.MainViewModel;
import com.vinhtt.PDFReader.util.ConfigLoader;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class MainController {

    @FXML private ScrollPane pdfContainer;
    @FXML private VBox pdfPageContainer;
    @FXML private ImageView pdfImageView; // New Injection

    @FXML private TextField pageInputField;
    @FXML private Label totalPagesLabel;

    @FXML private ListView<Paragraph> paragraphListView;
    @FXML private ListView<Sentence> sentenceListView;
    @FXML private Label statusLabel;

    private final MainViewModel viewModel = new MainViewModel();

    @FXML
    public void initialize() {
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Font Size Handler
        viewModel.appFontSizeProperty().addListener((obs, oldVal, newVal) ->
                updateAppFontSize(newVal.intValue()));
        pdfContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) updateAppFontSize(viewModel.appFontSizeProperty().get());
        });

        // --- List View Setup ---
        paragraphListView.setCellFactory(param -> new ParagraphTile(viewModel::translateParagraph));
        // FIX: Bind vào visibleParagraphList thay vì paragraphList
        paragraphListView.itemsProperty().bind(viewModel.visibleParagraphListProperty());

        sentenceListView.setCellFactory(param -> new SentenceTile(viewModel::analyzeSentence));
        sentenceListView.itemsProperty().bind(viewModel.sentenceListProperty());

        // Selection Event
        paragraphListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.selectedParagraphProperty().set(newVal);
            if (newVal != null) viewModel.loadSentencesFor(newVal);
        });

        // --- PDF Image Binding ---
        // Bind Image View với property currentPdfPageImage
        pdfImageView.imageProperty().bind(viewModel.currentPdfPageImageProperty());
        // Bind chiều rộng ảnh để responsive
        pdfImageView.fitWidthProperty().bind(pdfContainer.widthProperty().subtract(20));

        // --- Pagination Binding ---
        viewModel.totalPagesProperty().addListener((obs, oldVal, newVal) ->
                totalPagesLabel.setText("/ " + newVal));

        viewModel.currentPageProperty().addListener((obs, oldVal, newVal) ->
                pageInputField.setText(String.valueOf(newVal.intValue() + 1))); // Hiển thị 1-based

        // Xử lý nhập số trang
        pageInputField.setOnAction(e -> {
            try {
                int page = Integer.parseInt(pageInputField.getText()) - 1;
                viewModel.goToPage(page);
            } catch (NumberFormatException ex) {
                pageInputField.setText(String.valueOf(viewModel.currentPageProperty().get() + 1));
            }
        });
    }

    private void updateAppFontSize(int size) {
        if (pdfContainer.getScene() != null) {
            pdfContainer.getScene().getRoot().setStyle("-fx-font-size: " + size + "px;");
        }
    }

    // UI Actions
    public void onPrevPage() { viewModel.prevPage(); }
    public void onNextPage() { viewModel.nextPage(); }

    public void onOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(pdfContainer.getScene().getWindow());
        if (file != null) viewModel.loadPdf(file);
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
        apiKeyField.setPromptText("API Key");
        TextField modelField = new TextField(ConfigLoader.getGeminiModel());
        modelField.setPromptText("Model ID");
        TextArea promptArea = new TextArea(ConfigLoader.getTranslationPrompt());
        promptArea.setPrefRowCount(3);
        TextArea analysisPromptArea = new TextArea(ConfigLoader.getAnalysisPrompt());
        analysisPromptArea.setPrefRowCount(3);
        Spinner<Integer> fontSizeSpinner = new Spinner<>(10, 30, ConfigLoader.getFontSize());
        fontSizeSpinner.setEditable(true);

        grid.add(new Label("API Key:"), 0, 0); grid.add(apiKeyField, 1, 0);
        grid.add(new Label("Model:"), 0, 1); grid.add(modelField, 1, 1);
        grid.add(new Label("Trans Prompt:"), 0, 2); grid.add(promptArea, 1, 2);
        grid.add(new Label("Analysis Prompt:"), 0, 3); grid.add(analysisPromptArea, 1, 3);
        grid.add(new Label("Font Size:"), 0, 4); grid.add(fontSizeSpinner, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                int newSize = fontSizeSpinner.getValue();
                ConfigLoader.saveSettings(apiKeyField.getText(), modelField.getText(), promptArea.getText(), analysisPromptArea.getText(), newSize);
                viewModel.setAppFontSize(newSize);
                return saveButtonType;
            }
            return null;
        });
        dialog.showAndWait();
    }
}