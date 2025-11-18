package com.vinhtt.PDFReader.app;

import atlantafx.base.theme.PrimerDark;
import com.vinhtt.PDFReader.util.ConfigLoader;
import com.vinhtt.PDFReader.view.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main Entry Point for the Smart English PDF Reader application.
 */
public class App extends Application {

    private FXMLLoader fxmlLoader;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        // 1. Apply Dark Theme (AtlantaFX)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. Load Main View
        fxmlLoader = new FXMLLoader(App.class.getResource("/com/vinhtt/PDFReader/view/MainView.fxml"));

        // 3. Restore Window Size from Config
        double width = ConfigLoader.getWindowWidth();
        double height = ConfigLoader.getWindowHeight();
        Scene scene = new Scene(fxmlLoader.load(), width, height);

        // 4. Customize Stage
        stage.setTitle("Smart English PDF Reader");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Save Window Size
        if (primaryStage != null) {
            ConfigLoader.saveWindowSize(primaryStage.getWidth(), primaryStage.getHeight());
        }

        // Save UI State (Divider Positions)
        if (fxmlLoader != null) {
            MainController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.saveUiState();
            }
        }

        super.stop();
        System.out.println("Application stopping...");
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}