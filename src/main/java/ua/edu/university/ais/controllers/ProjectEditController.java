package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.edu.university.ais.models.InvestmentProject;

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

    private Stage dialogStage;
    private InvestmentProject project;
    private boolean saveClicked = false;

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

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public InvestmentProject getProject() {
        return project;
    }

    public void setProject(InvestmentProject project) {
        this.project = project;

        if (project != null) {
            idField.setText(project.getId());
            nameField.setText(project.getName());
            descriptionArea.setText(project.getDescription());
            investmentField.setText(Double.toString(project.getInitialInvestment()));
            statusComboBox.setValue(project.getStatus());
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (project == null) {
                project = new InvestmentProject(
                        idField.getText(),
                        nameField.getText(),
                        descriptionArea.getText(),
                        Double.parseDouble(investmentField.getText()),
                        statusComboBox.getValue()
                );
            } else {
                project.setId(idField.getText());
                project.setName(nameField.getText());
                project.setDescription(descriptionArea.getText());
                project.setInitialInvestment(Double.parseDouble(investmentField.getText()));
                project.setStatus(statusComboBox.getValue());
            }

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

        if (idField.getText() == null || idField.getText().isEmpty()) {
            errorMessage += "Не вказано ID!\n";
        }
        if (nameField.getText() == null || nameField.getText().isEmpty()) {
            errorMessage += "Не вказано назву!\n";
        }
        if (investmentField.getText() == null || investmentField.getText().isEmpty()) {
            errorMessage += "Не вказано інвестиції!\n";
        } else {
            try {
                Double.parseDouble(investmentField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Сума інвестицій має бути числом!\n";
            }
        }
        if (statusComboBox.getValue() == null || statusComboBox.getValue().isEmpty()) {
            errorMessage += "Не обрано статус!\n";
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