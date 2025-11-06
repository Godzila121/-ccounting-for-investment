package ua.edu.university.ais.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.edu.university.ais.models.Scenario;

import java.util.Locale;

public class ScenarioEditController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField incomeModifierField;
    @FXML
    private TextField costModifierField;

    private Stage dialogStage;
    private Scenario scenario;
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

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;

        if (scenario != null) {
            nameField.setText(scenario.getScenarioName());
            incomeModifierField.setText(String.format(Locale.US, "%.1f", scenario.getIncomeModifier() * 100.0));
            costModifierField.setText(String.format(Locale.US, "%.1f", scenario.getCostModifier() * 100.0));
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            String name = nameField.getText();
            double incomeMod = Double.parseDouble(incomeModifierField.getText().replace(',', '.')) / 100.0;
            double costMod = Double.parseDouble(costModifierField.getText().replace(',', '.')) / 100.0;

            if (scenario == null) {
                scenario = new Scenario(0, "", name, incomeMod, costMod);
            } else {
                scenario.setScenarioName(name);
                scenario.setIncomeModifier(incomeMod);
                scenario.setCostModifier(costMod);
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

        if (nameField.getText() == null || nameField.getText().isEmpty()) {
            errorMessage += "Не вказано назву сценарію!\n";
        }
        try {
            Double.parseDouble(incomeModifierField.getText().replace(',', '.'));
        } catch (NumberFormatException e) {
            errorMessage += "Модифікатор доходу має бути числом (наприклад, 110.0)!\n";
        }
        try {
            Double.parseDouble(costModifierField.getText().replace(',', '.'));
        } catch (NumberFormatException e) {
            errorMessage += "Модифікатор витрат має бути числом (наприклад, 105.0)!\n";
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