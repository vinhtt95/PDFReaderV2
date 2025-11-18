package com.vinhtt.PDFReader.view;

import com.vinhtt.PDFReader.model.Paragraph;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

public class MainController {

    @FXML
    private ScrollPane pdfContainer;

    @FXML
    private ListView<Paragraph> paragraphListView;

    @FXML
    private TextArea analysisArea;

    @FXML
    public void initialize() {
        // Tạo dữ liệu giả để test giao diện
        Paragraph p1 = new Paragraph(1, "The essential software requirement", 100.0f);
        Paragraph p2 = new Paragraph(2, "Hello, Phil? This is Maria in Human Resources.", 150.0f);

        paragraphListView.getItems().addAll(p1, p2);

        // Xử lý sự kiện khi click vào item
        paragraphListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                analysisArea.setText("Analysis for: " + newVal.getOriginalText() + "\n\n[Gemini Analysis Loading...]");
            }
        });
    }
}