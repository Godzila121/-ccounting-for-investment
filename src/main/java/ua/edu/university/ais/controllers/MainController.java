package ua.edu.university.ais.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ua.edu.university.ais.models.InvestmentProject;

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

    private final ObservableList<InvestmentProject> projectData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colInvestment.setCellValueFactory(cellData -> cellData.getValue().initialInvestmentProperty());

        projectData.add(new InvestmentProject("P-001", "Модернізація лінії А", "Оновлення обладнання", 1500000, "Планується"));
        projectData.add(new InvestmentProject("P-002", "Запуск нового продукту", "Виведення на ринок", 4500000, "Виконується"));
        projectData.add(new InvestmentProject("P-003", "IT-інфраструктура", "Оновлення серверів", 750000, "Завершено"));

        projectsTable.setItems(projectData);
    }
}