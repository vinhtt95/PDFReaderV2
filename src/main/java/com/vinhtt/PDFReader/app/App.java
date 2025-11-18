package com.vinhtt.PDFReader.app;

import atlantafx.base.theme.PrimerDark;
import com.vinhtt.PDFReader.util.ConfigLoader;
import com.vinhtt.PDFReader.view.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
// Thêm import cho AWT (hỗ trợ macOS Dock)
import java.awt.Taskbar;
import java.awt.Toolkit;

/**
 * Main Entry Point for the Smart English PDF Reader application.
 */
public class App extends Application {

    private FXMLLoader fxmlLoader;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        // 1. Apply Dark Theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. Load Main View
        fxmlLoader = new FXMLLoader(App.class.getResource("/com/vinhtt/PDFReader/view/MainView.fxml"));

        // 3. Restore Window Size
        double width = ConfigLoader.getWindowWidth();
        double height = ConfigLoader.getWindowHeight();
        Scene scene = new Scene(fxmlLoader.load(), width, height);

        // --- THÊM ĐOẠN NÀY ĐỂ SET ICON ---
        loadAppIcon(stage);
        // ---------------------------------

        // 4. Customize Stage
        stage.setTitle("Smart English PDF Reader");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Helper method to set application icon for both Windows/Linux and macOS Dock.
     */
    private void loadAppIcon(Stage stage) {
        try {
            // Đường dẫn tới file icon trong resources
            String iconPath = "/app_icon.png";

            // Kiểm tra xem file có tồn tại không để tránh lỗi NullPointerException
            if (App.class.getResource(iconPath) == null) {
                System.err.println("⚠️ Icon not found at: " + iconPath);
                return;
            }

            // 1. Set Icon cho Windows/Linux (Title bar & Taskbar)
            javafx.scene.image.Image fxIcon = new javafx.scene.image.Image(App.class.getResourceAsStream(iconPath));
            stage.getIcons().add(fxIcon);

            // 2. Set Icon cho macOS Dock (Sử dụng AWT Taskbar API)
            // Lưu ý: macOS Dock không tự nhận icon từ stage.getIcons() khi chạy từ IDE
            if (Taskbar.isTaskbarSupported()) {
                var taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    java.awt.Image awtIcon = Toolkit.getDefaultToolkit().getImage(App.class.getResource(iconPath));
                    taskbar.setIconImage(awtIcon);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Could not load app icon: " + e.getMessage());
        }
    }

    // ... (Giữ nguyên phần stop và main) ...
    @Override
    public void stop() throws Exception {
        if (primaryStage != null) {
            ConfigLoader.saveWindowSize(primaryStage.getWidth(), primaryStage.getHeight());
        }
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