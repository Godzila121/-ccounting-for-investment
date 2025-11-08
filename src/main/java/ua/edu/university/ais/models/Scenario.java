package ua.edu.university.ais.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Scenario {

    private final IntegerProperty scenarioId;
    private final StringProperty projectId;
    private final StringProperty scenarioName;
    // Поля incomeModifier та costModifier видалено

    public Scenario(int scenarioId, String projectId, String scenarioName) {
        this.scenarioId = new SimpleIntegerProperty(scenarioId);
        this.projectId = new SimpleStringProperty(projectId);
        this.scenarioName = new SimpleStringProperty(scenarioName);
    }

    public int getScenarioId() {
        return scenarioId.get();
    }

    public IntegerProperty scenarioIdProperty() {
        return scenarioId;
    }

    public String getProjectId() {
        return projectId.get();
    }

    public StringProperty projectIdProperty() {
        return projectId;
    }

    public String getScenarioName() {
        return scenarioName.get();
    }

    public StringProperty scenarioNameProperty() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName.set(scenarioName);
    }

    // Геттери та сеттери для incomeModifier/costModifier видалено

    @Override
    public String toString() {
        return this.getScenarioName();
    }
}