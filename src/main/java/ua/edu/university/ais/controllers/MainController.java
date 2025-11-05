package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.edu.university.ais.App;
import ua.edu.university.ais.models.InvestmentProject;

import java.io.IOException;
import java.util.Optional;
import javafx.scene.control.ButtonType;

public class MainController {

    @FXML
    private TableView<InvestmentProject> projectsTable;
    @FXML
    private TableColumn<InvestmentProject, String> colId;
    @FXML
    private TableColumn<InvestmentProject, String> colName;
    @FXML
    private TableColumn<InvestmentProject, String> colStatus;
    @FXML
    private TableColumn<InvestmentProject, Number> colInvestment;

    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;

    private final ObservableList<InvestmentProject> projectData = FXCollections.observableArrayList();
    private Stage primaryStage;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colInvestment.setCellValueFactory(cellData -> cellData.getValue().initialInvestmentProperty());

        editButton.setDisable(true);
        deleteButton.setDisable(true);

        projectsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showProjectButtons(newValue));

        loadTestData();
        projectsTable.setItems(projectData);
    }

    private void showProjectButtons(InvestmentProject project) {
        boolean projectSelected = project != null;
        editButton.setDisable(!projectSelected);
        deleteButton.setDisable(!projectSelected);
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void loadTestData() {
        projectData.add(new InvestmentProject("P-001", "Модернізація лінії А", "Оновлення обладнання", 1500000, "Планується"));
        projectData.add(new InvestmentProject("P-002", "Запуск нового продукту", "Виведення на ринок", 4500000, "Виконується"));
        projectData.add(new InvestmentProject("P-003", "IT-інфраструктура", "Оновлення серверів", 750000, "Завершено"));
    }

    @FXML
    private void handleAddNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/project-edit-view.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Новий проєкт");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProjectEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                projectData.add(controller.getProject());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProject() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці.", Alert.AlertType.WARNING);
            return;
        }

        // Логіка редагування буде додана тут на наступному кроці
        showAlert("В розробці", "Функціонал редагування буде додано.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleDeleteProject() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Підтвердження видалення");
            alert.setHeaderText("Видалення проєкту: " + selectedProject.getName());
            alert.setContentText("Ви впевнені, що хочете видалити цей проєкт?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                projectData.remove(selectedProject);
            }
        } else {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці.", Alert.AlertType.WARNING);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}