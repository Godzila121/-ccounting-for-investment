package ua.edu.university.ais.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PortfolioFund {

    private final StringProperty fundName;
    private final DoubleProperty initialBudget;
    private final DoubleProperty currentBudget;

    public PortfolioFund(String fundName, double initialBudget, double currentBudget) {
        this.fundName = new SimpleStringProperty(fundName);
        this.initialBudget = new SimpleDoubleProperty(initialBudget);
        this.currentBudget = new SimpleDoubleProperty(currentBudget);
    }

    public String getFundName() {
        return fundName.get();
    }

    public StringProperty fundNameProperty() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName.set(fundName);
    }

    public double getInitialBudget() {
        return initialBudget.get();
    }

    public DoubleProperty initialBudgetProperty() {
        return initialBudget;
    }

    public void setInitialBudget(double initialBudget) {
        this.initialBudget.set(initialBudget);
    }

    public double getCurrentBudget() {
        return currentBudget.get();
    }

    public DoubleProperty currentBudgetProperty() {
        return currentBudget;
    }

    public void setCurrentBudget(double currentBudget) {
        this.currentBudget.set(currentBudget);
    }
}