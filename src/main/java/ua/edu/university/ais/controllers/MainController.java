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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.edu.university.ais.App;
import ua.edu.university.ais.models.InvestmentProject;
import ua.edu.university.ais.models.PortfolioFund;
import ua.edu.university.ais.util.DatabaseHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

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
    private Button detailsButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button caseStudiesButton;
    @FXML
    private Button reportButton;
    @FXML
    private Button logoutButton;

    @FXML
    private Label totalBudgetLabel;
    @FXML
    private Label currentBudgetLabel;

    private final ObservableList<InvestmentProject> projectData = FXCollections.observableArrayList();
    private PortfolioFund portfolioFund;
    private Stage primaryStage;
    private App app;

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));

    public void setApp(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colInvestment.setCellValueFactory(cellData -> cellData.getValue().initialInvestmentProperty());

        detailsButton.setDisable(true);
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        reportButton.setDisable(true); // Звіт можна робити лише по обраному проєкту

        projectsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showProjectButtons(newValue));

        projectsTable.setItems(projectData);

        loadPortfolioFund();
        loadProjectsFromDatabase();
    }

    private void loadPortfolioFund() {
        String sql = "SELECT * FROM portfolio_funds WHERE fund_id = 1";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                this.portfolioFund = new PortfolioFund(
                        rs.getString("fund_name"),
                        rs.getDouble("initial_budget"),
                        rs.getDouble("current_budget")
                );
            } else {
                this.portfolioFund = new PortfolioFund("Помилка Завантаження", 0, 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.portfolioFund = new PortfolioFund("Помилка SQL", 0, 0);
            showAlert("Критична Помилка", "Не вдалося завантажити дані про бюджет.", Alert.AlertType.ERROR);
        }

        updateBudgetDisplay();
    }

    private void updateBudgetDisplay() {
        if (portfolioFund != null) {
            totalBudgetLabel.setText(currencyFormatter.format(portfolioFund.getInitialBudget()));
            currentBudgetLabel.setText(currencyFormatter.format(portfolioFund.getCurrentBudget()));
        }
    }

    private void showProjectButtons(InvestmentProject project) {
        boolean projectSelected = project != null;
        detailsButton.setDisable(!projectSelected);
        editButton.setDisable(!projectSelected);
        deleteButton.setDisable(!projectSelected);
        reportButton.setDisable(!projectSelected); // Активуємо кнопку звіту
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void loadProjectsFromDatabase() {
        projectData.clear();
        String sql = "SELECT * FROM projects";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                projectData.add(new InvestmentProject(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("initial_investment"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося завантажити проєкти.", Alert.AlertType.ERROR);
        }
    }

    private ProjectEditController showProjectEditDialog(InvestmentProject project, String title) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/project-edit-view.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProjectEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProject(project);

            dialogStage.showAndWait();

            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleAddNewProject() {
        ProjectEditController controller = showProjectEditDialog(null, "Новий проєкт");

        if (controller != null && controller.isSaveClicked()) {
            InvestmentProject newProject = controller.getProject();
            double requiredInvestment = newProject.getInitialInvestment();

            if (requiredInvestment > portfolioFund.getCurrentBudget()) {
                showAlert("Недостатньо коштів", "Немає грошей. Неможливо профінансувати проєкт.\n" +
                                "Потрібно: " + currencyFormatter.format(requiredInvestment) + "\n" +
                                "Залишок: " + currencyFormatter.format(portfolioFund.getCurrentBudget()),
                        Alert.AlertType.ERROR);
                return;
            }

            String sql = "INSERT INTO projects(id, name, description, initial_investment, status) VALUES(?,?,?,?,?)";

            try (Connection conn = DatabaseHandler.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, newProject.getId());
                pstmt.setString(2, newProject.getName());
                pstmt.setString(3, newProject.getDescription());
                pstmt.setDouble(4, newProject.getInitialInvestment());
                pstmt.setString(5, newProject.getStatus());
                pstmt.executeUpdate();

                projectData.add(newProject);
                updatePortfolioBudget(-requiredInvestment);

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Помилка Бази Даних", "Не вдалося додати проєкт.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleEditProject() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці.", Alert.AlertType.WARNING);
            return;
        }

        double oldInvestment = selectedProject.getInitialInvestment();

        ProjectEditController controller = showProjectEditDialog(selectedProject, "Редагування проєкту");

        if (controller != null && controller.isSaveClicked()) {
            double newInvestment = selectedProject.getInitialInvestment();
            double diff = newInvestment - oldInvestment;

            if (diff > portfolioFund.getCurrentBudget()) {
                showAlert("Недостатньо коштів", "Неможливо збільшити суму інвестицій.\n" +
                                "Потрібно додатково: " + currencyFormatter.format(diff) + "\n" +
                                "Залишок: " + currencyFormatter.format(portfolioFund.getCurrentBudget()),
                        Alert.AlertType.ERROR);
                loadProjectsFromDatabase();
                return;
            }

            String sql = "UPDATE projects SET name = ?, description = ?, initial_investment = ?, status = ? WHERE id = ?";

            try (Connection conn = DatabaseHandler.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, selectedProject.getName());
                pstmt.setString(2, selectedProject.getDescription());
                pstmt.setDouble(3, selectedProject.getInitialInvestment());
                pstmt.setString(4, selectedProject.getStatus());
                pstmt.setString(5, selectedProject.getId());
                pstmt.executeUpdate();

                projectsTable.refresh();
                if (diff != 0) {
                    updatePortfolioBudget(-diff);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Помилка Бази Даних", "Не вдалося оновити проєкт.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeleteProject() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Підтвердження видалення");
            alert.setHeaderText("Видалення проєкту: " + selectedProject.getName());
            alert.setContentText("Ви впевнені, що хочете видалити цей проєкт? " +
                    "Сума інвестицій (" + currencyFormatter.format(selectedProject.getInitialInvestment()) + ") буде повернута до бюджету.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String sql = "DELETE FROM projects WHERE id = ?";

                try (Connection conn = DatabaseHandler.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, selectedProject.getId());
                    pstmt.executeUpdate();

                    updatePortfolioBudget(selectedProject.getInitialInvestment());
                    projectData.remove(selectedProject);

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Помилка Бази Даних", "Не вдалося видалити проєкт.", Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці.", Alert.AlertType.WARNING);
        }
    }

    private void updatePortfolioBudget(double amountChange) {
        String sql = "UPDATE portfolio_funds SET current_budget = current_budget + ? WHERE fund_id = 1";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amountChange);
            pstmt.executeUpdate();

            loadPortfolioFund();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Критична Помилка", "Не вдалося оновити бюджет.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleShowDetails() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/project-details-view.fxml"));
            BorderPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Деталі проєкту: " + selectedProject.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProjectDetailsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProject(selectedProject);

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowCaseStudies() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/case-studies-view.fxml"));
            BorderPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("База знань (Приклади інвестицій)");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            CaseStudyController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ОСНОВНА ЗМІНА ТУТ ---
    @FXML
    private void handleShowReport() {
        InvestmentProject selectedProject = projectsTable.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть проєкт у таблиці для формування звіту.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/project-report-view.fxml")); // Новий FXML
            BorderPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Звіт по проєкту: " + selectedProject.getName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // "Оживляємо" звіт
            ReportController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            // Передаємо обраний проєкт у контролер звіту
            controller.setProject(selectedProject);

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Помилка", "Не вдалося завантажити звіт: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleLogout() {
        app.showLoginScreen();
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