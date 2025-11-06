package ua.edu.university.ais.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CashFlow {

    private final IntegerProperty flowId;
    private final StringProperty projectId;
    private final IntegerProperty year;
    private final DoubleProperty amount;
    private final StringProperty type;

    public CashFlow(int flowId, String projectId, int year, double amount, String type) {
        this.flowId = new SimpleIntegerProperty(flowId);
        this.projectId = new SimpleStringProperty(projectId);
        this.year = new SimpleIntegerProperty(year);
        this.amount = new SimpleDoubleProperty(amount);
        this.type = new SimpleStringProperty(type);
    }

    public int getFlowId() {
        return flowId.get();
    }

    public IntegerProperty flowIdProperty() {
        return flowId;
    }

    public void setFlowId(int flowId) {
        this.flowId.set(flowId);
    }

    public String getProjectId() {
        return projectId.get();
    }

    public StringProperty projectIdProperty() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId.set(projectId);
    }

    public int getYear() {
        return year.get();
    }

    public IntegerProperty yearProperty() {
        return year;
    }

    public void setYear(int year) {
        this.year.set(year);
    }

    public double getAmount() {
        return amount.get();
    }

    public DoubleProperty amountProperty() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount.set(amount);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }
}