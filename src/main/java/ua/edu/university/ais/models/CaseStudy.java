package ua.edu.university.ais.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CaseStudy {

    private final IntegerProperty caseId;
    private final StringProperty title;
    private final StringProperty description;
    private final StringProperty outcome;
    private final StringProperty lessonsLearned;

    public CaseStudy(int caseId, String title, String description, String outcome, String lessonsLearned) {
        this.caseId = new SimpleIntegerProperty(caseId);
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.outcome = new SimpleStringProperty(outcome);
        this.lessonsLearned = new SimpleStringProperty(lessonsLearned);
    }

    public int getCaseId() {
        return caseId.get();
    }

    public IntegerProperty caseIdProperty() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId.set(caseId);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public String getOutcome() {
        return outcome.get();
    }

    public StringProperty outcomeProperty() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome.set(outcome);
    }

    public String getLessonsLearned() {
        return lessonsLearned.get();
    }

    public StringProperty lessonsLearnedProperty() {
        return lessonsLearned;
    }

    public void setLessonsLearned(String lessonsLearned) {
        this.lessonsLearned.set(lessonsLearned);
    }

    @Override
    public String toString() {
        return this.getTitle();
    }
}