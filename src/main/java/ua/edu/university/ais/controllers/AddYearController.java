package ua.edu.university.ais.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddYearController {

    @FXML
    private TextField yearField;

    private Stage dialogStage;
    private int year;
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

    public int getYear() {
        return year;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            year = Integer.parseInt(yearField.getText());
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
        if (yearField.getText() == null || yearField.getText().isEmpty()) {
            errorMessage += "Не вказано рік!\n";
        } else {
            try {
                Integer.parseInt(yearField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Рік має бути цілим числом (напр., 2026)!\n";
            }
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