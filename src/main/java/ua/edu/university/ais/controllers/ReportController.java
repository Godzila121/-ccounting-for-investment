package ua.edu.university.ais.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ua.edu.university.ais.models.FlowDriver;
import ua.edu.university.ais.models.InvestmentProject;
import ua.edu.university.ais.models.Scenario;
import ua.edu.university.ais.util.DatabaseHandler;
import ua.edu.university.ais.util.FinancialCalculator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ReportController {

    @FXML
    private Label projectNameLabel;
    @FXML
    private ComboBox<Scenario> scenarioComboBox;
    @FXML
    private TableView<KpiReportRow> kpiReportTable;
    @FXML
    private TableColumn<KpiReportRow, String> colKpiName;
    @FXML
    private TableColumn<KpiReportRow, String> colPlan;
    @FXML
    private TableColumn<KpiReportRow, String> colForecast;
    @FXML
    private TableColumn<KpiReportRow, String> colActual;
    @FXML
    private TableColumn<KpiReportRow, String> colPlanVsActual;
    @FXML
    private TextArea reportSummaryArea;

    private Stage dialogStage;
    private InvestmentProject project;
    private final ObservableList<Scenario> scenarioData = FXCollections.observableArrayList();
    private final Scenario baseScenario = new Scenario(0, "0", "Базовий (План)");
    private List<FlowDriver> allDrivers; // Кеш драйверів

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));
    private final NumberFormat percentFormatter = NumberFormat.getPercentInstance(new Locale("uk", "UA"));

    @FXML
    private void initialize() {
        // Налаштування ComboBox
        scenarioComboBox.setItems(scenarioData);

        // Налаштування таблиці звіту
        colKpiName.setCellValueFactory(new PropertyValueFactory<>("kpiName"));
        colPlan.setCellValueFactory(new PropertyValueFactory<>("planValue"));
        colForecast.setCellValueFactory(new PropertyValueFactory<>("forecastValue"));
        colActual.setCellValueFactory(new PropertyValueFactory<>("actualValue"));
        colPlanVsActual.setCellValueFactory(new PropertyValueFactory<>("deviation"));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // Головний метод, який отримує дані з MainController
    public void setProject(InvestmentProject project) {
        this.project = project;
        projectNameLabel.setText("Звіт: " + project.getName());
        loadScenarios();

        // Завантажуємо всі драйвери проекту ОДИН раз
        allDrivers = getAllDriversForProject();
    }

    // Завантажує сценарії (Прогнози) для вибору
    private void loadScenarios() {
        scenarioData.clear();
        scenarioData.add(baseScenario); // "Базовий" це наш "План"

        String sql = "SELECT * FROM scenarios WHERE project_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, project.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                scenarioData.add(new Scenario(
                        rs.getInt("scenario_id"),
                        rs.getString("project_id"),
                        rs.getString("scenario_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- ЛОГІКА ФОРМУВАННЯ ЗВІТУ ---

    @FXML
    private void handleLoadReport() {
        // 1. Отримуємо обраний сценарій (Прогноз)
        Scenario forecastScenario = scenarioComboBox.getValue();
        if (forecastScenario == null) {
            showAlert("Помилка", "Будь ласка, оберіть сценарій для порівняння.", Alert.AlertType.WARNING);
            return;
        }

        // 2. Отримуємо дані для трьох стовпців
        KpiSet planKpis = calculateKpisForScenario(baseScenario);
        KpiSet forecastKpis = calculateKpisForScenario(forecastScenario);
        KpiSet actualKpis = calculateKpisForActuals();

        // 3. Заповнюємо таблицю
        ObservableList<KpiReportRow> reportData = FXCollections.observableArrayList();
        reportData.add(createReportRow("NPV (₴)", planKpis.npv, forecastKpis.npv, actualKpis.npv, true));
        reportData.add(createReportRow("IRR (%)", planKpis.irr, forecastKpis.irr, actualKpis.irr, false));
        reportData.add(createReportRow("Термін окуп. (р.)", planKpis.pp, forecastKpis.pp, actualKpis.pp, false));

        kpiReportTable.setItems(reportData);

        // 4. Генеруємо висновок
        generateReportSummary(planKpis, actualKpis);
    }

    // Внутрішній клас-контейнер для 4-х показників
    private static class KpiSet {
        double npv = 0.0;
        double irr = Double.NaN;
        double pp = Double.POSITIVE_INFINITY;
    }

    // Метод-хелпер для заповнення таблиці
    private KpiReportRow createReportRow(String name, double plan, double forecast, double actual, boolean isCurrency) {
        double deviation = actual - plan;

        // TODO: Додати форматування та кольори
        return new KpiReportRow(
                name,
                formatValue(plan, isCurrency),
                formatValue(forecast, isCurrency),
                formatValue(actual, isCurrency),
                formatValue(deviation, isCurrency)
        );
    }

    private String formatValue(double value, boolean isCurrency) {
        if(Double.isNaN(value)) return "Н/Д";
        if(Double.isInfinite(value)) return "Не окупається";
        if(isCurrency) return currencyFormatter.format(value);
        if(Math.abs(value) < 1) return percentFormatter.format(value); // Для IRR
        return String.format("%.2f", value); // Для PP/DPP
    }

    // --- ЛОГІКА РОЗРАХУНКІВ (ПЛАН, ПРОГНОЗ, ФАКТ) ---

    // 1. Розрахунок для ПЛАНУ (baseScenario) та ПРОГНОЗУ (forecastScenario)
    private KpiSet calculateKpisForScenario(Scenario scenario) {
        Map<Integer, List<FlowDriver>> driversByYear = allDrivers.stream()
                .collect(Collectors.groupingBy(FlowDriver::getYear, TreeMap::new, Collectors.toList()));

        TreeMap<Integer, Double> netFlowsByYear = new TreeMap<>();
        Map<Integer, Double> scenarioValues = loadScenarioValuesFromCache(scenario.getScenarioId());

        for (Map.Entry<Integer, List<FlowDriver>> entry : driversByYear.entrySet()) {
            int year = entry.getKey();
            Map<String, Double> driversMap = new HashMap<>();

            for (FlowDriver driver : entry.getValue()) {
                double value;
                if (scenario == baseScenario) {
                    value = driver.getBaseValue();
                } else {
                    value = scenarioValues.getOrDefault(driver.getDriverId(), driver.getBaseValue());
                }
                driversMap.put(driver.getDriverName(), value);
            }
            netFlowsByYear.put(year, FinancialCalculator.calculateNetCashFlow(driversMap));
        }

        // TODO: Отримати ставку дисконтування з GUI
        double discountRate = 0.10; // Поки що 10%

        KpiSet kpis = new KpiSet();
        kpis.npv = FinancialCalculator.calculateNPV(discountRate, netFlowsByYear);
        kpis.irr = FinancialCalculator.calculateIRR(netFlowsByYear);
        kpis.pp = FinancialCalculator.calculatePaybackPeriod(netFlowsByYear, false, 0);
        return kpis;
    }

    // 2. Розрахунок для ФАКТУ
    private KpiSet calculateKpisForActuals() {
        Map<Integer, List<FlowDriver>> driversByYear = allDrivers.stream()
                .collect(Collectors.groupingBy(FlowDriver::getYear, TreeMap::new, Collectors.toList()));

        TreeMap<Integer, Double> netFlowsByYear = new TreeMap<>();
        Map<Integer, Double> actualValues = loadActualValuesFromDb();

        for (Map.Entry<Integer, List<FlowDriver>> entry : driversByYear.entrySet()) {
            int year = entry.getKey();
            Map<String, Double> driversMap = new HashMap<>();

            for (FlowDriver driver : entry.getValue()) {
                // Беремо ФАКТИЧНЕ значення. Якщо його немає - беремо БАЗОВЕ (ПЛАН)
                double value = actualValues.getOrDefault(driver.getDriverId(), driver.getBaseValue());
                driversMap.put(driver.getDriverName(), value);
            }
            netFlowsByYear.put(year, FinancialCalculator.calculateNetCashFlow(driversMap));
        }

        double discountRate = 0.10; // Поки що 10%

        KpiSet kpis = new KpiSet();
        kpis.npv = FinancialCalculator.calculateNPV(discountRate, netFlowsByYear);
        kpis.irr = FinancialCalculator.calculateIRR(netFlowsByYear);
        kpis.pp = FinancialCalculator.calculatePaybackPeriod(netFlowsByYear, false, 0);
        return kpis;
    }

    // 3. Генерування висновку
    private void generateReportSummary(KpiSet plan, KpiSet actual) {
        StringBuilder summary = new StringBuilder();
        summary.append("АНАЛІТИЧНИЙ ВИСНОВОК:\n\n");

        double npvDiff = actual.npv - plan.npv;

        if (npvDiff > 0) {
            summary.append(String.format(
                    "Проєкт демонструє КРАЩУ за очікувану ефективність.\n" +
                            "Фактичний NPV (%s) перевищив плановий (%s) на %s.\n",
                    formatValue(actual.npv, true),
                    formatValue(plan.npv, true),
                    formatValue(npvDiff, true)
            ));
            summary.append("ВИСНОВОК: Проєкт успішний.");
        } else if (npvDiff < 0) {
            summary.append(String.format(
                    "Проєкт демонструє ГІРШУ за очікувану ефективність.\n" +
                            "Фактичний NPV (%s) нижчий за плановий (%s) на %s.\n",
                    formatValue(actual.npv, true),
                    formatValue(plan.npv, true),
                    formatValue(npvDiff, true)
            ));
            if (actual.npv < 0) {
                summary.append("ВИСНОВОК: Проєкт провальний (фактичний NPV від'ємний).");
            } else {
                summary.append("ВИСНОВОК: Проєкт збитковий відносно плану, але залишається прибутковим.");
            }
        } else {
            summary.append("Проєкт розвивається чітко за планом. Відхилення відсутні.");
        }

        reportSummaryArea.setText(summary.toString());
    }

    // --- Допоміжні методи ---

    private Map<Integer, Double> loadScenarioValuesFromCache(int scenarioId) {
        Map<Integer, Double> values = new HashMap<>();
        String sql = "SELECT driver_id, scenario_value FROM flow_scenario_values WHERE scenario_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, scenarioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                values.put(rs.getInt("driver_id"), rs.getDouble("scenario_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    private Map<Integer, Double> loadActualValuesFromDb() {
        Map<Integer, Double> values = new HashMap<>();
        String sql = "SELECT driver_id, actual_value FROM actual_data";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                values.put(rs.getInt("driver_id"), rs.getDouble("actual_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    private List<FlowDriver> getAllDriversForProject() {
        List<FlowDriver> drivers = new ArrayList<>();
        String sql = "SELECT * FROM flow_drivers WHERE project_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, project.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                drivers.add(new FlowDriver(
                        rs.getInt("driver_id"),
                        rs.getString("project_id"),
                        rs.getString("driver_name"),
                        rs.getString("driver_type"),
                        rs.getInt("year"),
                        rs.getDouble("base_value")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return drivers;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Внутрішній клас-модель для таблиці звіту ---
    public static class KpiReportRow {
        private final SimpleStringProperty kpiName;
        private final SimpleStringProperty planValue;
        private final SimpleStringProperty forecastValue;
        private final SimpleStringProperty actualValue;
        private final SimpleStringProperty deviation;

        public KpiReportRow(String kpiName, String planValue, String forecastValue, String actualValue, String deviation) {
            this.kpiName = new SimpleStringProperty(kpiName);
            this.planValue = new SimpleStringProperty(planValue);
            this.forecastValue = new SimpleStringProperty(forecastValue);
            this.actualValue = new SimpleStringProperty(actualValue);
            this.deviation = new SimpleStringProperty(deviation);
        }

        public String getKpiName() { return kpiName.get(); }
        public String getPlanValue() { return planValue.get(); }
        public String getForecastValue() { return forecastValue.get(); }
        public String getActualValue() { return actualValue.get(); }
        public String getDeviation() { return deviation.get(); }
    }
}