module com.vinhtt.PDFReader {
    requires javafx.controls;
    requires javafx.fxml;

    // Thư viện giao diện
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // Thư viện xử lý
    requires org.apache.pdfbox;
    requires java.sql;             // Cho SQLite
    requires okhttp3;              // Cho Gemini API
    requires com.google.gson;      // Cho JSON
    requires org.slf4j;            // Cho Logging

    // Mở gói view để JavaFX FXML loader có thể truy cập
    opens com.vinhtt.PDFReader.view to javafx.fxml;
    opens com.vinhtt.PDFReader.app to javafx.fxml;

    // Mở gói model để Gson có thể serialize/deserialize
    opens com.vinhtt.PDFReader.model to com.google.gson;

    exports com.vinhtt.PDFReader.app;
}