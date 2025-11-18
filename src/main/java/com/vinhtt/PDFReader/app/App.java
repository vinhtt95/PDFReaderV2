package com.vinhtt.PDFReader.app;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Apply Dark Theme (AtlantaFX)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. Load Main View
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/vinhtt/PDFReader/view/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

        // 3. Customize Stage
        stage.setTitle("Smart English PDF Reader");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}