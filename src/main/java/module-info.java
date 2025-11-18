module com.vinhtt.PDFReader {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing; // Cần thiết cho SwingFXUtils (Render PDF)

    // Thư viện giao diện
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // Thư viện xử lý
    requires org.apache.pdfbox;
    requires java.sql;
    requires okhttp3;
    requires com.google.gson;
    requires org.slf4j;
    requires java.prefs;
    requires java.desktop; // Cần thiết cho BufferedImage

    opens com.vinhtt.PDFReader.view to javafx.fxml;
    opens com.vinhtt.PDFReader.app to javafx.fxml;
    opens com.vinhtt.PDFReader.model to com.google.gson;

    exports com.vinhtt.PDFReader.app;
}