package ua.edu.university.ais.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Scenario {

    private final IntegerProperty scenarioId;
    private final StringProperty projectId;
    private final StringProperty scenarioName;
    private final DoubleProperty incomeModifier;
    private final DoubleProperty costModifier;

    public Scenario(int scenarioId, String projectId, String scenarioName, double incomeModifier, double costModifier) {
        this.scenarioId = new SimpleIntegerProperty(scenarioId);
        this.projectId = new SimpleStringProperty(projectId);
        this.scenarioName = new SimpleStringProperty(scenarioName);
        this.incomeModifier = new SimpleDoubleProperty(incomeModifier);
        this.costModifier = new SimpleDoubleProperty(costModifier);
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

    public double getIncomeModifier() {
        return incomeModifier.get();
    }

    public DoubleProperty incomeModifierProperty() {
        return incomeModifier;
    }

    public void setIncomeModifier(double incomeModifier) {
        this.incomeModifier.set(incomeModifier);
    }

    public double getCostModifier() {
        return costModifier.get();
    }

    public DoubleProperty costModifierProperty() {
        return costModifier;
    }

    public void setCostModifier(double costModifier) {
        this.costModifier.set(costModifier);
    }

    @Override
    public String toString() {
        return this.getScenarioName();
    }
}