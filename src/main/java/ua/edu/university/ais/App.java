package ua.edu.university.ais;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ua.edu.university.ais.controllers.MainController;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("views/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        MainController controller = fxmlLoader.getController();
        controller.setPrimaryStage(stage);

        stage.setTitle("АІС Облік прогнозування інвестиційних програм");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}