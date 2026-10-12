package com.fnc;

import com.fnc.ui.MainFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the grammar converter application.
 * Context-Free Grammar converter to Chomsky Normal Form (FNC) and
 * Greibach Normal Form (FNG) with step-by-step visualization.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Conversor de gramáticas — FNC y FNG");

        MainFrame root = new MainFrame();
        Scene scene = new Scene(root, 1100, 700);
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        root.registerAccelerators(scene);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
