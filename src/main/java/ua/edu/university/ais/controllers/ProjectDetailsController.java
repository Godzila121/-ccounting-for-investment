package ua.edu.university.ais.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.edu.university.ais.App;
import ua.edu.university.ais.models.FlowDriver;
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
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDetailsController {

    // FXML Поля
    @FXML
    private Label projectNameLabel;
    @FXML
    private TextField discountRateField;
    @FXML
    private ComboBox<Scenario> scenarioComboBox;
    @FXML
    private Button newScenarioButton;
    @FXML
    private Button editScenarioButton; // Ми залишили цю кнопку, але вона вимкнена
    @FXML
    private Button deleteScenarioButton;
    @FXML
    private Button calculateButton;
    @FXML
    private Button addDriverButton;
    @FXML
    private Button addYearButton;
    @FXML
    private TableView<Map<String, Object>> driverTableView;
    @FXML
    private BarChart<String, Number> cashFlowChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;
    @FXML
    private Label npvLabel;
    @FXML
    private Label irrLabel;
    @FXML
    private Label ppLabel;
    @FXML
    private Label dppLabel;
    @FXML
    private TextArea projectDescriptionArea;

    // Внутрішні змінні
    private Stage dialogStage;
    private InvestmentProject project;
    private final ObservableList<Scenario> scenarioData = FXCollections.observableArrayList();
    private final Scenario baseScenario = new Scenario(0, "0", "Базовий сценарій");
    private List<Integer> projectYears = new ArrayList<>();

    // Мапа для зберігання значень сценаріїв: <ScenarioID, <DriverID, Value>>
    private Map<Integer, Map<Integer, Double>> scenarioValueCache = new HashMap<>();

    // Форматери
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));
    private final NumberFormat percentFormatter = NumberFormat.getPercentInstance(new Locale("uk", "UA"));
    private final DecimalFormat yearsFormatter = new DecimalFormat("#.##");

    // --- Ініціалізація та завантаження ---
    @FXML
    private void initialize() {
        scenarioComboBox.setItems(scenarioData);
        scenarioData.add(baseScenario);
        scenarioComboBox.setValue(baseScenario);

        editScenarioButton.setDisable(true); // Редагування сценаріїв відбувається у сітці
        deleteScenarioButton.setDisable(true);

        // Слухач для ComboBox: перезавантажує сітку при зміні сценарію
        scenarioComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean isBaseOrNull = (newValue == null || newValue == baseScenario);
                    deleteScenarioButton.setDisable(isBaseOrNull);
                    loadDriverData(); // Перезавантажити сітку
                }
        );

        cashFlowChart.setLegendVisible(false);
        projectDescriptionArea.setEditable(false);
        projectDescriptionArea.setWrapText(true);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProject(InvestmentProject project) {
        this.project = project;
        projectNameLabel.setText(project.getName());
        projectDescriptionArea.setText(project.getDescription());

        loadScenarios(); // Завантажуємо сценарії
        loadScenarioValuesIntoCache(); // Завантажуємо ВСІ значення сценаріїв у пам'ять
        buildDriverTable(); // Будуємо стовпці
        loadDriverData(); // Заповнюємо рядки
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
                        rs.getString("scenario_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося завантажити сценарії.", Alert.AlertType.ERROR);
        }
        scenarioComboBox.setValue(baseScenario);
    }

    private void loadScenarioValuesIntoCache() {
        scenarioValueCache.clear();
        String sql = "SELECT fsv.scenario_id, fsv.driver_id, fsv.scenario_value " +
                "FROM flow_scenario_values fsv " +
                "JOIN flow_drivers fd ON fsv.driver_id = fd.driver_id " +
                "WHERE fd.project_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, project.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int scenarioId = rs.getInt("scenario_id");
                int driverId = rs.getInt("driver_id");
                double value = rs.getDouble("scenario_value");

                scenarioValueCache.computeIfAbsent(scenarioId, k -> new HashMap<>()).put(driverId, value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Логіка "Excel-подібної" сітки (Driver Table) ---

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

    private List<String> getDriverNames() {
        return getAllDriversForProject().stream()
                .map(FlowDriver::getDriverName)
                .distinct()
                .collect(Collectors.toList());
    }

    private void buildDriverTable() {
        driverTableView.getColumns().clear();
        driverTableView.setEditable(true);

        List<FlowDriver> allDrivers = getAllDriversForProject();
        projectYears = allDrivers.stream()
                .map(FlowDriver::getYear)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        TableColumn<Map<String, Object>, String> driverNameCol = new TableColumn<>("Драйвер");
        driverNameCol.setPrefWidth(150);
        driverNameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("driver_name").toString()));
        driverNameCol.setEditable(false);
        driverTableView.getColumns().add(driverNameCol);

        TableColumn<Map<String, Object>, String> driverTypeCol = new TableColumn<>("Тип");
        driverTypeCol.setPrefWidth(80);
        driverTypeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get("driver_type").toString()));
        driverTypeCol.setEditable(false);
        driverTableView.getColumns().add(driverTypeCol);

        for (Integer year : projectYears) {
            TableColumn<Map<String, Object>, String> yearCol = new TableColumn<>(year.toString());
            yearCol.setPrefWidth(90);

            yearCol.setCellValueFactory(param -> {
                String key = "year_" + year;
                Object value = param.getValue().get(key);
                if (value == null) {
                    return new SimpleStringProperty("0.0");
                }
                return new SimpleStringProperty(value.toString());
            });

            yearCol.setCellFactory(TextFieldTableCell.forTableColumn());

            yearCol.setOnEditCommit(event -> {
                Map<String, Object> row = event.getRowValue();

                if (row.get("driver_id_" + year) == null) {
                    showAlert("Помилка", "Неможливо оновити неіснуючий драйвер. (ID не знайдено).", Alert.AlertType.WARNING);
                    driverTableView.refresh();
                    return;
                }

                int driverId = (int) row.get("driver_id_" + year);
                double newValue;
                try {
                    newValue = Double.parseDouble(event.getNewValue().replace(',', '.'));
                } catch (NumberFormatException e) {
                    newValue = Double.parseDouble(event.getOldValue().replace(',', '.'));
                }

                updateDriverValue(driverId, newValue);
                row.put("year_" + year, newValue);
            });

            driverTableView.getColumns().add(yearCol);
        }
    }

    private void loadDriverData() {
        ObservableList<Map<String, Object>> tableData = FXCollections.observableArrayList();
        List<FlowDriver> allDrivers = getAllDriversForProject();
        Scenario selectedScenario = scenarioComboBox.getValue();

        Map<String, List<FlowDriver>> driversByName = allDrivers.stream()
                .collect(Collectors.groupingBy(FlowDriver::getDriverName));

        for (Map.Entry<String, List<FlowDriver>> entry : driversByName.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("driver_name", entry.getKey());
            row.put("driver_type", entry.getValue().get(0).getDriverType());

            for (FlowDriver driver : entry.getValue()) {
                double valueToDisplay;

                if (selectedScenario == null || selectedScenario == baseScenario) {
                    valueToDisplay = driver.getBaseValue();
                } else {
                    valueToDisplay = scenarioValueCache
                            .getOrDefault(selectedScenario.getScenarioId(), Collections.emptyMap())
                            .getOrDefault(driver.getDriverId(), driver.getBaseValue());
                }

                row.put("year_" + driver.getYear(), valueToDisplay);
                row.put("driver_id_" + driver.getYear(), driver.getDriverId());
            }

            for (Integer year : projectYears) {
                if (!row.containsKey("year_" + year)) {
                    row.put("year_" + year, 0.0);
                }
            }

            tableData.add(row);
        }
        driverTableView.setItems(tableData);
    }

    private void updateDriverValue(int driverId, double newValue) {
        Scenario selectedScenario = scenarioComboBox.getValue();

        if (selectedScenario == baseScenario) {
            String sql = "UPDATE flow_drivers SET base_value = ? WHERE driver_id = ?";
            try (Connection conn = DatabaseHandler.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, newValue);
                pstmt.setInt(2, driverId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Помилка Бази Даних", "Не вдалося оновити базове значення.", Alert.AlertType.ERROR);
            }
        } else {
            String sql = "INSERT INTO flow_scenario_values (scenario_id, driver_id, scenario_value) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE scenario_value = ?";
            try (Connection conn = DatabaseHandler.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selectedScenario.getScenarioId());
                pstmt.setInt(2, driverId);
                pstmt.setDouble(3, newValue);
                pstmt.setDouble(4, newValue);
                pstmt.executeUpdate();

                scenarioValueCache.computeIfAbsent(selectedScenario.getScenarioId(), k -> new HashMap<>()).put(driverId, newValue);

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Помилка Бази Даних", "Не вдалося оновити значення сценарію.", Alert.AlertType.ERROR);
            }
        }
    }

    // --- Обробники кнопок ---

    @FXML
    private void handleAddDriver() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/add-driver-view.fxml"));
            AnchorPane page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Додати новий драйвер");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            AddDriverController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                String name = controller.getDriverName();
                String type = controller.getDriverType();

                // --- ВИПРАВЛЕННЯ ---
                // Якщо років ще немає, беремо поточний рік як рік за замовчуванням
                List<Integer> yearsToUpdate = projectYears;
                if (yearsToUpdate.isEmpty()) {
                    yearsToUpdate = List.of(Calendar.getInstance().get(Calendar.YEAR));
                }
                // --- КІНЕЦЬ ВИПРАВЛЕННЯ ---

                String sql = "INSERT INTO flow_drivers (project_id, driver_name, driver_type, year, base_value) VALUES (?, ?, ?, ?, 0.0)";
                try (Connection conn = DatabaseHandler.getConnection()) {
                    for (Integer year : yearsToUpdate) { // Використовуємо оновлений список
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, project.getId());
                            pstmt.setString(2, name);
                            pstmt.setString(3, type);
                            pstmt.setInt(4, year);
                            pstmt.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Помилка Бази Даних", "Не вдалося додати драйвер.", Alert.AlertType.ERROR);
                }

                buildDriverTable();
                loadDriverData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddYear() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/add-year-view.fxml"));
            AnchorPane page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Додати новий рік");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            AddYearController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                int year = controller.getYear();

                if (projectYears.contains(year)) {
                    showAlert("Помилка", "Рік " + year + " вже існує.", Alert.AlertType.WARNING);
                    return;
                }

                // --- ВИПРАВЛЕННЯ ---
                List<String> driverNames = getDriverNames();
                Map<String, List<FlowDriver>> driversByName = getAllDriversForProject().stream().collect(Collectors.groupingBy(FlowDriver::getDriverName));

                // Якщо драйверів ще немає, створюємо "базовий" набір
                if (driverNames.isEmpty()) {
                    driverNames = List.of("Кількість продажів", "Ціна за одиницю", "Собівартість одиниці", "Інвестиція");
                    // Створюємо "фейкові" дані, щоб отримати тип
                    driversByName = Map.of(
                            "Кількість продажів", List.of(new FlowDriver(0,"","","INCOME",0,0)),
                            "Ціна за одиницю", List.of(new FlowDriver(0,"","","INCOME",0,0)),
                            "Собівартість одиниці", List.of(new FlowDriver(0,"","","COST",0,0)),
                            "Інвестиція", List.of(new FlowDriver(0,"","","COST",0,0))
                    );
                }
                // --- КІНЕЦЬ ВИПРАВЛЕННЯ ---

                String sql = "INSERT INTO flow_drivers (project_id, driver_name, driver_type, year, base_value) VALUES (?, ?, ?, ?, 0.0)";

                try (Connection conn = DatabaseHandler.getConnection()) {
                    for (String name : driverNames) {
                        String type = driversByName.get(name).get(0).getDriverType();
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, project.getId());
                            pstmt.setString(2, name);
                            pstmt.setString(3, type);
                            pstmt.setInt(4, year);
                            pstmt.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Помилка Бази Даних", "Не вдалося додати рік.", Alert.AlertType.ERROR);
                }

                buildDriverTable();
                loadDriverData();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

        List<FlowDriver> allDrivers = getAllDriversForProject();
        Scenario selectedScenario = scenarioComboBox.getValue();

        Map<Integer, List<FlowDriver>> driversByYear = allDrivers.stream()
                .collect(Collectors.groupingBy(FlowDriver::getYear, TreeMap::new, Collectors.toList()));

        TreeMap<Integer, Double> netFlowsByYear = new TreeMap<>();

        for (Map.Entry<Integer, List<FlowDriver>> entry : driversByYear.entrySet()) {
            int year = entry.getKey();
            List<FlowDriver> yearDrivers = entry.getValue();

            Map<String, Double> driversMap = new HashMap<>();
            for (FlowDriver driver : yearDrivers) {
                double value;
                if (selectedScenario == baseScenario) {
                    value = driver.getBaseValue();
                } else {
                    value = scenarioValueCache
                            .getOrDefault(selectedScenario.getScenarioId(), Collections.emptyMap())
                            .getOrDefault(driver.getDriverId(), driver.getBaseValue());
                }
                driversMap.put(driver.getDriverName(), value);
            }

            double ncf = FinancialCalculator.calculateNetCashFlow(driversMap);
            netFlowsByYear.put(year, ncf);
        }

        updateCashFlowChart(netFlowsByYear);

        double npv = FinancialCalculator.calculateNPV(discountRate, netFlowsByYear);
        double irr = FinancialCalculator.calculateIRR(netFlowsByYear);
        double pp = FinancialCalculator.calculatePaybackPeriod(netFlowsByYear, false, 0);
        double dpp = FinancialCalculator.calculatePaybackPeriod(netFlowsByYear, true, discountRate);

        npvLabel.setText(currencyFormatter.format(npv));
        if (Double.isNaN(irr)) {
            irrLabel.setText("Н/Д (N/A)");
        } else {
            irrLabel.setText(percentFormatter.format(irr));
        }
        ppLabel.setText(formatPaybackPeriod(pp));
        dppLabel.setText(formatPaybackPeriod(dpp));
    }

    private void updateCashFlowChart(TreeMap<Integer, Double> netFlowsByYear) {
        cashFlowChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (netFlowsByYear.isEmpty()) return;
        int startYear = netFlowsByYear.firstKey();

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            int normalizedYear = actualYear - startYear;
            String xAxisLabel = String.format("%d (Рік %d)", actualYear, normalizedYear);
            series.getData().add(new XYChart.Data<>(xAxisLabel, entry.getValue()));
        }
        cashFlowChart.getData().add(series);
    }

    // --- Управління сценаріями (НОВА ЛОГІКА) ---

    @FXML
    private void handleNewScenario() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/add-scenario-view.fxml"));
            AnchorPane page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Новий сценарій");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.dialogStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            AddScenarioController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                String name = controller.getScenarioName();
                int newScenarioId = createScenarioInDb(name);
                if (newScenarioId == -1) return;

                copyBaseValuesToScenario(newScenarioId);

                loadScenarios();
                loadScenarioValuesIntoCache();
                for (Scenario s : scenarioData) {
                    if (s.getScenarioId() == newScenarioId) {
                        scenarioComboBox.setValue(s);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int createScenarioInDb(String name) {
        String sql = "INSERT INTO scenarios(project_id, scenario_name) VALUES(?,?)";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, project.getId());
            pstmt.setString(2, name);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося створити сценарій.", Alert.AlertType.ERROR);
        }
        return -1;
    }

    private void copyBaseValuesToScenario(int newScenarioId) {
        String sql = "INSERT INTO flow_scenario_values (scenario_id, driver_id, scenario_value) " +
                "SELECT ?, driver_id, base_value " +
                "FROM flow_drivers " +
                "WHERE project_id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newScenarioId);
            pstmt.setString(2, project.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося скопіювати базові значення для сценарію.", Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void handleEditScenario() {
        showAlert("Редагування", "Редагуйте значення для обраного сценарію прямо у сітці.", Alert.AlertType.INFORMATION);
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
        alert.setContentText("Ви впевнені? Всі дані цього сценарію будуть втрачені.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteScenarioFromDb(selectedScenario);
            scenarioData.remove(selectedScenario);
            scenarioComboBox.setValue(baseScenario);
        }
    }

    private void deleteScenarioFromDb(Scenario scenario) {
        String sqlValues = "DELETE FROM flow_scenario_values WHERE scenario_id = ?";
        String sqlScenario = "DELETE FROM scenarios WHERE scenario_id = ?";

        try (Connection conn = DatabaseHandler.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtValues = conn.prepareStatement(sqlValues)) {
                pstmtValues.setInt(1, scenario.getScenarioId());
                pstmtValues.executeUpdate();
            }

            try (PreparedStatement pstmtScenario = conn.prepareStatement(sqlScenario)) {
                pstmtScenario.setInt(1, scenario.getScenarioId());
                pstmtScenario.executeUpdate();
            }

            conn.commit();

            scenarioValueCache.remove(scenario.getScenarioId());

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка Бази Даних", "Не вдалося видалити сценарій.", Alert.AlertType.ERROR);
        }
    }

    // --- Допоміжні методи ---

    private String formatPaybackPeriod(double period) {
        if (Double.isInfinite(period)) {
            return "Не окупається";
        }
        if (period <= 0) {
            return "0 (одразу)";
        }
        return yearsFormatter.format(period) + " р.";
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