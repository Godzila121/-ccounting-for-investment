package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.edu.university.ais.models.CaseStudy;

public class CaseStudyEditController {

    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<String> outcomeComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea lessonsLearnedArea;

    private Stage dialogStage;
    private CaseStudy caseStudy;
    private boolean saveClicked = false;

    @FXML
    private void initialize() {
        outcomeComboBox.setItems(FXCollections.observableArrayList(
                "Успіх",
                "Провал",
                "Нейтрально"
        ));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public CaseStudy getCaseStudy() {
        return caseStudy;
    }

    public void setCaseStudy(CaseStudy caseStudy) {
        this.caseStudy = caseStudy;

        if (caseStudy != null) {
            titleField.setText(caseStudy.getTitle());
            outcomeComboBox.setValue(caseStudy.getOutcome());
            descriptionArea.setText(caseStudy.getDescription());
            lessonsLearnedArea.setText(caseStudy.getLessonsLearned());
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (caseStudy == null) {
                caseStudy = new CaseStudy(
                        0, // ID буде згенеровано БД
                        titleField.getText(),
                        descriptionArea.getText(),
                        outcomeComboBox.getValue(),
                        lessonsLearnedArea.getText()
                );
            } else {
                caseStudy.setTitle(titleField.getText());
                caseStudy.setDescription(descriptionArea.getText());
                caseStudy.setOutcome(outcomeComboBox.getValue());
                caseStudy.setLessonsLearned(lessonsLearnedArea.getText());
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

        if (titleField.getText() == null || titleField.getText().isEmpty()) {
            errorMessage += "Не вказано назву!\n";
        }
        if (outcomeComboBox.getValue() == null || outcomeComboBox.getValue().isEmpty()) {
            errorMessage += "Не обрано результат!\n";
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