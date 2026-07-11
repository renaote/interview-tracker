package com.renate.tracker;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // App icon - shows in the taskbar and window title bar
        Image icon = new Image(getClass().getResourceAsStream("/images/app-icon.png"));
        primaryStage.getIcons().add(icon);

        // Splash screen: a small borderless window shown briefly while the
        // real UI loads. The DB init + FXML load are actually near-instant,
        // so this is mostly cosmetic - but it gives the app a more "finished
        // product" feel instead of just popping straight into the main window.
        Stage splash = buildSplashStage(icon);
        splash.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(1.4));
        delay.setOnFinished(event -> {
            try {
                showMainWindow(primaryStage, icon);
                splash.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load main window", e);
            }
        });
        delay.play();
    }

    private Stage buildSplashStage(Image icon) {
        Stage splash = new Stage(StageStyle.UNDECORATED);
        splash.getIcons().add(icon);

        Label title = new Label("Interview & Internship Prep Tracker");
        title.setStyle("-fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        spinner.setStyle("-fx-progress-color: white;");

        VBox root = new VBox(16, title, spinner);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1e3a8a; -fx-padding: 40;");

        Scene scene = new Scene(root, 420, 220);
        splash.setScene(scene);
        splash.centerOnScreen();
        return splash;
    }

    private void showMainWindow(Stage primaryStage, Image icon) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/renate/tracker/view/main.fxml"));
        Parent root = loader.load();

        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("Interview & Internship Prep Tracker");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}