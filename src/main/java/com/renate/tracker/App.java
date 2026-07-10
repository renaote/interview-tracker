package com.renate.tracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// This is the file that actually starts the app.
// It just loads the FXML layout and puts it on screen.
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/renate/tracker/view/main.fxml"));
        Parent root = loader.load();

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