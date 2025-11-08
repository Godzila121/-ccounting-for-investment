package ua.edu.university.ais.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class FlowScenarioValue {

    private final IntegerProperty valueId;
    private final IntegerProperty scenarioId;
    private final IntegerProperty driverId;
    private final DoubleProperty scenarioValue;

    public FlowScenarioValue(int valueId, int scenarioId, int driverId, double scenarioValue) {
        this.valueId = new SimpleIntegerProperty(valueId);
        this.scenarioId = new SimpleIntegerProperty(scenarioId);
        this.driverId = new SimpleIntegerProperty(driverId);
        this.scenarioValue = new SimpleDoubleProperty(scenarioValue);
    }

    public int getValueId() {
        return valueId.get();
    }

    public IntegerProperty valueIdProperty() {
        return valueId;
    }

    public int getScenarioId() {
        return scenarioId.get();
    }

    public IntegerProperty scenarioIdProperty() {
        return scenarioId;
    }

    public int getDriverId() {
        return driverId.get();
    }

    public IntegerProperty driverIdProperty() {
        return driverId;
    }

    public double getScenarioValue() {
        return scenarioValue.get();
    }

    public DoubleProperty scenarioValueProperty() {
        return scenarioValue;
    }

    public void setScenarioValue(double scenarioValue) {
        this.scenarioValue.set(scenarioValue);
    }
}