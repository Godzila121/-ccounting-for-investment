package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.edu.university.ais.App;
import ua.edu.university.ais.models.CashFlow;
import ua.edu.university.ais.models.InvestmentProject;
import ua.edu.university.ais.models.Scenario;
import ua.edu.university.ais.util.DatabaseHandler;
import ua.edu.university.ais.util.FinancialCalculator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ProjectDetailsController {

    @FXML
    private Label projectNameLabel;
    @FXML
    private TextField discountRateField;
    @FXML
    private ComboBox<Scenario> scenarioComboBox;
    @FXML
    private Button newScenarioButton;
    @FXML
    private Button editScenarioButton;
    @FXML
    private Button deleteScenarioButton;
    @FXML
    private Button calculateButton;
    @FXML
    private TableView<CashFlow> cashFlowTable;
    @FXML
    private TableColumn<CashFlow, Number> colYear;
    @FXML
    private TableColumn<CashFlow, Number> colAmount;
    @FXML
    private TableColumn<CashFlow, String> colType;
    @FXML
    private TextField yearField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField typeField;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label npvLabel;
    @FXML
    private Label irrLabel;
    @FXML
    private Label ppLabel;
    @FXML
    private Label dppLabel;

    @FXML
    private BarChart<String, Number> cashFlowChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;

    private Stage dialogStage;
    private InvestmentProject project;
    private final ObservableList<CashFlow> cashFlowData = FXCollections.observableArrayList();
    private final ObservableList<Scenario> scenarioData = FXCollections.observableArrayList();
    private final Scenario baseScenario = new Scenario(0, "0", "Базовий сценарій", 1.0, 1.0);

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));
    private final NumberFormat percentFormatter = NumberFormat.getPercentInstance(new Locale("uk", "UA"));
    private final DecimalFormat yearsFormatter = new DecimalFormat("#.##");

    @FXML
    private void initialize() {
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());

        cashFlowTable.setItems(cashFlowData);
        percentFormatter.setMinimumFractionDigits(2);

        scenarioComboBox.setItems(scenarioData);
        scenarioData.add(baseScenario);
        scenarioComboBox.setValue(baseScenario);

        editScenarioButton.setDisable(true);
        deleteScenarioButton.setDisable(true);
        scenarioComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean isBaseOrNull = (newValue == null || newValue == baseScenario);
                    editScenarioButton.setDisable(isBaseOrNull);
                    deleteScenarioButton.setDisable(isBaseOrNull);
                }
        );

        cashFlowChart.setLegendVisible(false);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProject(InvestmentProject project) {
        this.project = project;
        projectNameLabel.setText(project.getName());
        loadCashFlows();
        loadScenarios();
    }

    private void loadCashFlows() {
        cashFlowData.clear();
        String sql = "SELECT * FROM cash_flows WHERE project_id = ? ORDER BY year";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, project.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                cashFlowData.add(new CashFlow(
                        rs.getInt("flow_id"),
                        rs.getString("project_id"),
                        rs.getInt("year"),
                        rs.getDouble("amount"),
                        rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося завантажити грошові потоки.", Alert.AlertType.ERROR);
        }
        updateCashFlowChart();
    }

    private void loadScenarios() {
        scenarioData.clear();
        scenarioData.add(baseScenario);
        String sql = "SELECT * FROM scenarios WHERE project_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, project.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                scenarioData.add(new Scenario(
                        rs.getInt("scenario_id"),
                        rs.getString("project_id"),
                        rs.getString("scenario_name"),
                        rs.getDouble("income_modifier"),
                        rs.getDouble("cost_modifier")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося завантажити сценарії.", Alert.AlertType.ERROR);
        }
        scenarioComboBox.setValue(baseScenario);
    }

    private void updateCashFlowChart() {
        cashFlowChart.getData().clear();

        TreeMap<Integer, Double> netFlowsByYear = cashFlowData.stream()
                .collect(Collectors.groupingBy(
                        CashFlow::getYear,
                        TreeMap::new,
                        Collectors.summingDouble(CashFlow::getAmount)
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        if (netFlowsByYear.isEmpty()) {
            return;
        }

        int startYear = netFlowsByYear.firstKey();

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            int normalizedYear = actualYear - startYear;
            String xAxisLabel = String.format("%d (Рік %d)", actualYear, normalizedYear);

            series.getData().add(new XYChart.Data<>(xAxisLabel, entry.getValue()));
        }

        cashFlowChart.getData().add(series);
    }

    @FXML
    private void handleAddNewFlow() {
        if (!validateFlowInput()) {
            return;
        }

        String sql = "INSERT INTO cash_flows(project_id, year, amount, type) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, project.getId());
            pstmt.setInt(2, Integer.parseInt(yearField.getText()));
            pstmt.setDouble(3, Double.parseDouble(amountField.getText().replace(',', '.')));
            pstmt.setString(4, typeField.getText());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        CashFlow newFlow = new CashFlow(
                                newId,
                                project.getId(),
                                Integer.parseInt(yearField.getText()),
                                Double.parseDouble(amountField.getText().replace(',', '.')),
                                typeField.getText()
                        );
                        cashFlowData.add(newFlow);
                        clearFlowInputFields();
                        updateCashFlowChart();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося додати потік.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteFlow() {
        CashFlow selectedFlow = cashFlowTable.getSelectionModel().getSelectedItem();
        if (selectedFlow == null) {
            showAlert("Нічого не вибрано", "Будь ласка, виберіть потік зі списку.", Alert.AlertType.WARNING);
            return;
        }

        String sql = "DELETE FROM cash_flows WHERE flow_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selectedFlow.getFlowId());
            pstmt.executeUpdate();
            cashFlowData.remove(selectedFlow);
            updateCashFlowChart();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося видалити потік.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCalculate() {
        double discountRate;
        try {
            discountRate = Double.parseDouble(discountRateField.getText().replace(',', '.')) / 100.0;
        } catch (NumberFormatException e) {
            showAlert("Помилка в даних", "Ставка дисконтування має бути числом!", Alert.AlertType.ERROR);
            return;
        }

        if (cashFlowData.isEmpty()) {
            showAlert("Немає даних", "Додайте грошові потоки для розрахунку.", Alert.AlertType.WARNING);
            return;
        }

        Scenario selectedScenario = scenarioComboBox.getValue();
        if (selectedScenario == null) {
            selectedScenario = baseScenario;
        }

        List<CashFlow> modifiedFlows = new ArrayList<>();
        for (CashFlow flow : cashFlowData) {
            double newAmount = flow.getAmount();
            String type = flow.getType().toLowerCase();

            if (type.equals("дохід") || newAmount > 0) {
                newAmount *= selectedScenario.getIncomeModifier();
            } else if (type.equals("витрати") || type.equals("інвестиція") || newAmount < 0) {
                newAmount *= selectedScenario.getCostModifier();
            }

            modifiedFlows.add(new CashFlow(
                    flow.getFlowId(), flow.getProjectId(), flow.getYear(), newAmount, flow.getType()
            ));
        }

        double npv = FinancialCalculator.calculateNPV(discountRate, modifiedFlows);
        double irr = FinancialCalculator.calculateIRR(modifiedFlows);
        double pp = FinancialCalculator.calculatePaybackPeriod(modifiedFlows, false, 0);
        double dpp = FinancialCalculator.calculatePaybackPeriod(modifiedFlows, true, discountRate);

        npvLabel.setText(currencyFormatter.format(npv));

        if (Double.isNaN(irr)) {
            irrLabel.setText("Н/Д (N/A)");
        } else {
            irrLabel.setText(percentFormatter.format(irr));
        }

        ppLabel.setText(formatPaybackPeriod(pp));
        dppLabel.setText(formatPaybackPeriod(dpp));
    }

    private ScenarioEditController showScenarioEditDialog(Scenario scenario, String title) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("views/scenario-edit-view.fxml"));
            AnchorPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ScenarioEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setScenario(scenario);

            dialogStage.showAndWait();

            return controller;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleNewScenario() {
        ScenarioEditController controller = showScenarioEditDialog(null, "Новий сценарій");

        if (controller != null && controller.isSaveClicked()) {
            Scenario newScenario = controller.getScenario();
            saveScenarioToDb(newScenario);
            loadScenarios();
            scenarioComboBox.setValue(newScenario);
        }
    }

    @FXML
    private void handleEditScenario() {
        Scenario selectedScenario = scenarioComboBox.getValue();
        if (selectedScenario == null || selectedScenario == baseScenario) {
            showAlert("Неможливо редагувати", "Базовий сценарій не можна редагувати.", Alert.AlertType.WARNING);
            return;
        }

        ScenarioEditController controller = showScenarioEditDialog(selectedScenario, "Редагувати сценарій");

        if (controller != null && controller.isSaveClicked()) {
            updateScenarioInDb(selectedScenario);
            loadScenarios();
            scenarioComboBox.setValue(selectedScenario);
        }
    }

    @FXML
    private void handleDeleteScenario() {
        Scenario selectedScenario = scenarioComboBox.getValue();
        if (selectedScenario == null || selectedScenario == baseScenario) {
            showAlert("Неможливо видалити", "Базовий сценарій не можна видалити.", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(dialogStage);
        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Видалити сценарій: " + selectedScenario.getScenarioName());
        alert.setContentText("Ви впевнені?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteScenarioFromDb(selectedScenario);
            scenarioData.remove(selectedScenario);
            scenarioComboBox.setValue(baseScenario);
        }
    }

    private void saveScenarioToDb(Scenario scenario) {
        String sql = "INSERT INTO scenarios(project_id, scenario_name, income_modifier, cost_modifier) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, project.getId());
            pstmt.setString(2, scenario.getScenarioName());
            pstmt.setDouble(3, scenario.getIncomeModifier());
            pstmt.setDouble(4, scenario.getCostModifier());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося зберегти сценарій.", Alert.AlertType.ERROR);
        }
    }

    private void updateScenarioInDb(Scenario scenario) {
        String sql = "UPDATE scenarios SET scenario_name = ?, income_modifier = ?, cost_modifier = ? WHERE scenario_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, scenario.getScenarioName());
            pstmt.setDouble(2, scenario.getIncomeModifier());
            pstmt.setDouble(3, scenario.getCostModifier());
            pstmt.setInt(4, scenario.getScenarioId());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося оновити сценарій.", Alert.AlertType.ERROR);
        }
    }

    private void deleteScenarioFromDb(Scenario scenario) {
        String sql = "DELETE FROM scenarios WHERE scenario_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, scenario.getScenarioId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося видалити сценарій.", Alert.AlertType.ERROR);
        }
    }

    private String formatPaybackPeriod(double period) {
        if (Double.isInfinite(period)) {
            return "Не окупається";
        }
        if (period <= 0) {
            return "0 (одразу)";
        }
        return yearsFormatter.format(period) + " р.";
    }

    private boolean validateFlowInput() {
        String errorMessage = "";
        try {
            Integer.parseInt(yearField.getText());
        } catch (NumberFormatException e) {
            errorMessage += "Рік має бути цілим числом (напр. 2024)!\n";
        }
        try {
            Double.parseDouble(amountField.getText().replace(',', '.'));
        } catch (NumberFormatException e) {
            errorMessage += "Сума має бути числом!\n";
        }
        if (typeField.getText() == null || typeField.getText().isEmpty()) {
            errorMessage += "Не вказано тип!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("Помилка в даних", errorMessage, Alert.AlertType.ERROR);
            return false;
        }
    }

    private void clearFlowInputFields() {
        yearField.clear();
        amountField.clear();
        typeField.setText("Дохід");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}