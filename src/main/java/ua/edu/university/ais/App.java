package ua.edu.university.ais;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ua.edu.university.ais.controllers.LoginController;
import ua.edu.university.ais.controllers.MainController;

import java.io.IOException;

public class App extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        // Вмикаємо тему AtlantaFX Primer Dark
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        primaryStage.setTitle("АІС Облік прогнозування інвестиційних програм");

        // Починаємо з екрану логіну
        showLoginScreen();

        primaryStage.show();
    }

    public void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/login-view.fxml"));
            VBox loginLayout = loader.load();

            LoginController controller = loader.getController();
            controller.setApp(this);

            Scene scene = new Scene(loginLayout, 400, 300);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/main-view.fxml"));
            BorderPane mainLayout = loader.load();

            MainController controller = loader.getController();
            controller.setApp(this);
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(mainLayout, 950, 600);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}