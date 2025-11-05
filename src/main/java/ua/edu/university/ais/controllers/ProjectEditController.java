package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ProjectEditController {

    @FXML
    private TextField idField;

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField investmentField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Stage dialogStage;

    @FXML
    private void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList(
                "Планується",
                "Виконується",
                "Завершено",
                "Заморожено"
        ));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleSave() {
        // Логіка збереження буде тут
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}