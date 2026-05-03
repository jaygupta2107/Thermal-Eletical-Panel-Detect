package com.example.thermal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static boolean opencvLoaded = false;

    static {
        loadOpenCV();
    }

    private static void loadOpenCV() {
        try {
            System.load("/usr/lib/jni/libopencv_java454d.so");
            opencvLoaded = true;
            System.out.println("OpenCV loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            opencvLoaded = false;
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    public static boolean isOpenCVLoaded() {
        return opencvLoaded;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/thermal/dashboard.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/example/thermal/dashboard.css").toExternalForm());

        primaryStage.setTitle("Thermal Panel Monitoring Dashboard");
        primaryStage.setScene(scene);

        // Responsive sizing to 90% screen size, centered
        javafx.stage.Screen primaryScreen = javafx.stage.Screen.getPrimary();
        javafx.geometry.Rectangle2D screenBounds = primaryScreen.getBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        double targetWidth = screenWidth * 0.9;
        double targetHeight = screenHeight * 0.9;

        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(750);
        primaryStage.setWidth(targetWidth);
        primaryStage.setHeight(targetHeight);
        primaryStage.centerOnScreen();
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
