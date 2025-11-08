package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.edu.university.ais.App;
import ua.edu.university.ais.models.CaseStudy;
import ua.edu.university.ais.util.DatabaseHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class CaseStudyController {

    @FXML
    private ListView<CaseStudy> caseListView;
    @FXML
    private Button newButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label titleLabel;
    @FXML
    private Label outcomeLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea lessonsLearnedArea;

    private Stage dialogStage;
    private final ObservableList<CaseStudy> caseData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        caseListView.setItems(caseData);

        editButton.setDisable(true);
        deleteButton.setDisable(true);

        caseListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showCaseDetails(newValue));

        loadCaseStudiesFromDatabase();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void loadCaseStudiesFromDatabase() {
        caseData.clear();
        String sql = "SELECT * FROM case_studies";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                caseData.add(new CaseStudy(
                        rs.getInt("case_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("outcome"),
                        rs.getString("lessons_learned")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Помилка Бази Даних", "Не вдалося завантажити приклади.");
        }
    }

    private void showCaseDetails(CaseStudy caseStudy) {
        if (caseStudy != null) {
            titleLabel.setText(caseStudy.getTitle());
            outcomeLabel.setText(caseStudy.getOutcome());
            descriptionArea.setText(caseStudy.getDescription());
            lessonsLearnedArea.setText(caseStudy.getLessonsLearned());

            editButton.setDisable(false);
            deleteButton.setDisable(false);
        } else {
            titleLabel.setText("-");
            outcomeLabel.setText("-");
            descriptionArea.clear();
            lessonsLearnedArea.clear();

            editButton.setDisable(true);
            deleteButton.setDisable(true);
        }
    }

    private CaseStudyEditController showCaseEditDialog(CaseStudy caseStudy, String title) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/case-study-edit-view.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            CaseStudyEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setCaseStudy(caseStudy);

            dialogStage.showAndWait();

            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleNewCase() {
        CaseStudyEditController controller = showCaseEditDialog(null, "Новий приклад");
        if (controller != null && controller.isSaveClicked()) {
            CaseStudy newCase = controller.getCaseStudy();
            saveCaseToDb(newCase);
            loadCaseStudiesFromDatabase();
            caseListView.getSelectionModel().select(newCase);
        }
    }

    @FXML
    private void handleEditCase() {
        CaseStudy selectedCase = caseListView.getSelectionModel().getSelectedItem();
        if (selectedCase == null) {
            showAlert(Alert.AlertType.WARNING, "Нічого не вибрано", "Будь ласка, виберіть приклад зі списку.");
            return;
        }

        CaseStudyEditController controller = showCaseEditDialog(selectedCase, "Редагувати приклад");
        if (controller != null && controller.isSaveClicked()) {
            updateCaseInDb(selectedCase);
            loadCaseStudiesFromDatabase(); // Оновлюємо весь список
            caseListView.getSelectionModel().select(selectedCase);
        }
    }

    @FXML
    private void handleDeleteCase() {
        CaseStudy selectedCase = caseListView.getSelectionModel().getSelectedItem();
        if (selectedCase == null) {
            showAlert(Alert.AlertType.WARNING, "Нічого не вибрано", "Будь ласка, виберіть приклад зі списку.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(dialogStage);
        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Видалити приклад: " + selectedCase.getTitle());
        alert.setContentText("Ви впевнені?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteCaseFromDb(selectedCase);
            loadCaseStudiesFromDatabase(); // Оновлюємо список
        }
    }

    private void saveCaseToDb(CaseStudy caseStudy) {
        String sql = "INSERT INTO case_studies(title, description, outcome, lessons_learned) VALUES(?,?,?,?)";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, caseStudy.getTitle());
            pstmt.setString(2, caseStudy.getDescription());
            pstmt.setString(3, caseStudy.getOutcome());
            pstmt.setString(4, caseStudy.getLessonsLearned());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Помилка Бази Даних", "Не вдалося зберегти приклад.");
        }
    }

    private void updateCaseInDb(CaseStudy caseStudy) {
        String sql = "UPDATE case_studies SET title = ?, description = ?, outcome = ?, lessons_learned = ? WHERE case_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, caseStudy.getTitle());
            pstmt.setString(2, caseStudy.getDescription());
            pstmt.setString(3, caseStudy.getOutcome());
            pstmt.setString(4, caseStudy.getLessonsLearned());
            pstmt.setInt(5, caseStudy.getCaseId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Помилка Бази Даних", "Не вдалося оновити приклад.");
        }
    }

    private void deleteCaseFromDb(CaseStudy caseStudy) {
        String sql = "DELETE FROM case_studies WHERE case_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, caseStudy.getCaseId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Помилка Бази Даних", "Не вдалося видалити приклад.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}