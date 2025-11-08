package ua.edu.university.ais.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FlowDriver {

    private final IntegerProperty driverId;
    private final StringProperty projectId;
    private final StringProperty driverName;
    private final StringProperty driverType; // "INCOME" або "COST"
    private final IntegerProperty year;
    private final DoubleProperty baseValue;

    public FlowDriver(int driverId, String projectId, String driverName, String driverType, int year, double baseValue) {
        this.driverId = new SimpleIntegerProperty(driverId);
        this.projectId = new SimpleStringProperty(projectId);
        this.driverName = new SimpleStringProperty(driverName);
        this.driverType = new SimpleStringProperty(driverType);
        this.year = new SimpleIntegerProperty(year);
        this.baseValue = new SimpleDoubleProperty(baseValue);
    }

    public int getDriverId() {
        return driverId.get();
    }

    public IntegerProperty driverIdProperty() {
        return driverId;
    }

    public String getProjectId() {
        return projectId.get();
    }

    public StringProperty projectIdProperty() {
        return projectId;
    }

    public String getDriverName() {
        return driverName.get();
    }

    public StringProperty driverNameProperty() {
        return driverName;
    }

    public String getDriverType() {
        return driverType.get();
    }

    public StringProperty driverTypeProperty() {
        return driverType;
    }

    public int getYear() {
        return year.get();
    }

    public IntegerProperty yearProperty() {
        return year;
    }

    public double getBaseValue() {
        return baseValue.get();
    }

    public DoubleProperty baseValueProperty() {
        return baseValue;
    }

    public void setBaseValue(double baseValue) {
        this.baseValue.set(baseValue);
    }
}