package com.vinhtt.PDFReader.view;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.view.components.ParagraphTile;
import com.vinhtt.PDFReader.viewmodel.MainViewModel;
import com.vinhtt.PDFReader.util.ConfigLoader;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

public class MainController {

    @FXML private ScrollPane pdfContainer;
    @FXML private ListView<Paragraph> paragraphListView;
    @FXML private TextArea analysisArea;
    @FXML private Label statusLabel; // Bạn cần thêm Label này vào FXML nếu muốn hiển thị status

    private final MainViewModel viewModel = new MainViewModel();

    @FXML
    public void initialize() {
        // 1. Setup ListView with Custom Cell
        paragraphListView.setCellFactory(param -> new ParagraphTile(viewModel::translateParagraph));

        // 2. Bind Data
        paragraphListView.itemsProperty().bind(viewModel.paragraphListProperty());

        // 3. Listen to Selection
        paragraphListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.selectedParagraphProperty().set(newVal);
            if (newVal != null) {
                analysisArea.setText("Original: " + newVal.getOriginalText());
            }
        });

        // 4. Setup Menu Actions (Quick implementation via lookup or binding)
        // Note: In real app, define menuItem fx:id and set onAction here
    }

    // Method called from FXML Menu "Open PDF..."
    public void onOpenPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(pdfContainer.getScene().getWindow());
        if (file != null) {
            viewModel.loadPdf(file);
        }
    }

    // Method called from FXML Menu "Settings"
    public void onOpenSettings() {
        TextInputDialog dialog = new TextInputDialog(ConfigLoader.getApiKey());
        dialog.setTitle("Settings");
        dialog.setHeaderText("Gemini API Configuration");
        dialog.setContentText("Enter API Key:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ConfigLoader::saveApiKey);
    }
}