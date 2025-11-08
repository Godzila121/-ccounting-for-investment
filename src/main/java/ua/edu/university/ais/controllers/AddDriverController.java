package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddDriverController {

    @FXML
    private TextField driverNameField;
    @FXML
    private ComboBox<String> driverTypeComboBox;

    private Stage dialogStage;
    private String driverName;
    private String driverType;
    private boolean saveClicked = false;

    @FXML
    private void initialize() {
        // INCOME - Дохід (позитивний вплив), COST - Витрата (негативний вплив)
        driverTypeComboBox.setItems(FXCollections.observableArrayList(
                "INCOME",
                "COST"
        ));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverType() {
        return driverType;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            driverName = driverNameField.getText();
            driverType = driverTypeComboBox.getValue();
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

        if (driverNameField.getText() == null || driverNameField.getText().isEmpty()) {
            errorMessage += "Не вказано назву драйвера!\n";
        }
        if (driverTypeComboBox.getValue() == null || driverTypeComboBox.getValue().isEmpty()) {
            errorMessage += "Не обрано тип драйвера!\n";
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