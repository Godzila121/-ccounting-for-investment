package ua.edu.university.ais.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddScenarioController {

    @FXML
    private TextField scenarioNameField;

    private Stage dialogStage;
    private String scenarioName;
    private boolean saveClicked = false;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            scenarioName = scenarioNameField.getText();
            saveClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (scenarioNameField.getText() == null || scenarioNameField.getText().isEmpty()) {
            errorMessage += "Не вказано назву сценарію!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Помилка в даних");
            alert.setHeaderText("Будь ласка, виправте некоректні поля");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}