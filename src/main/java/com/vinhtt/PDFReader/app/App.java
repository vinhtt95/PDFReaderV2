package com.vinhtt.PDFReader.app;

import atlantafx.base.theme.PrimerDark;
import com.vinhtt.PDFReader.view.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private FXMLLoader fxmlLoader;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Apply Dark Theme (AtlantaFX)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. Load Main View
        fxmlLoader = new FXMLLoader(App.class.getResource("/com/vinhtt/PDFReader/view/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1440, 1000);

        // 3. Customize Stage
        stage.setTitle("Smart English PDF Reader");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // Đóng sạch sẽ mọi thứ
        System.out.println("Application stopping...");

        // (Tùy chọn) Nếu có truy cập được ViewModel từ đây thì gọi storageService.close()
        // Nhưng cách đơn giản nhất để kill hết daemon thread là exit
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}