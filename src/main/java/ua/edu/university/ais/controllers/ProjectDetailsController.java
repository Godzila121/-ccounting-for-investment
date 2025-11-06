package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.edu.university.ais.models.CashFlow;
import ua.edu.university.ais.models.InvestmentProject;
import ua.edu.university.ais.util.DatabaseHandler;
import ua.edu.university.ais.util.FinancialCalculator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ProjectDetailsController {

    @FXML
    private Label projectNameLabel;
    @FXML
    private TextField discountRateField;
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

    private Stage dialogStage;
    private InvestmentProject project;
    private final ObservableList<CashFlow> cashFlowData = FXCollections.observableArrayList();
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
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProject(InvestmentProject project) {
        this.project = project;
        projectNameLabel.setText(project.getName());
        loadCashFlows();
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
    }

    @FXML
    private void handleAddNewFlow() {
        if (!validateFlowInput()) {
            return;
        }

        String sql = "INSERT INTO cash_flows(project_id, year, amount, type) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, project.getId());
            pstmt.setInt(2, Integer.parseInt(yearField.getText()));
            pstmt.setDouble(3, Double.parseDouble(amountField.getText().replace(',', '.')));
            pstmt.setString(4, typeField.getText());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        cashFlowData.add(new CashFlow(
                                newId,
                                project.getId(),
                                Integer.parseInt(yearField.getText()),
                                Double.parseDouble(amountField.getText().replace(',', '.')),
                                typeField.getText()
                        ));
                        clearFlowInputFields();
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

        double npv = FinancialCalculator.calculateNPV(discountRate, cashFlowData);
        double irr = FinancialCalculator.calculateIRR(cashFlowData);
        double pp = FinancialCalculator.calculatePaybackPeriod(cashFlowData, false, 0);
        double dpp = FinancialCalculator.calculatePaybackPeriod(cashFlowData, true, discountRate);

        npvLabel.setText(currencyFormatter.format(npv));

        if (Double.isNaN(irr)) {
            irrLabel.setText("Н/Д (N/A)");
        } else {
            irrLabel.setText(percentFormatter.format(irr));
        }

        ppLabel.setText(formatPaybackPeriod(pp));
        dppLabel.setText(formatPaybackPeriod(dpp));
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
            errorMessage += "Рік має бути числом!\n";
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