package com.renate.tracker.model;

import java.time.LocalDate;

// This is one row in my tracker - one company/application.
public class Company {

    private int id;
    private String name;
    private String roleTitle;
    private Stage stage;
    private LocalDate deadline;
    private String notes;
    private String applicationUrl;

    public Company() {
        // new applications start at the first stage by default
        this.stage = Stage.APPLIED;
    }

    public Company(int id, String name, String roleTitle, Stage stage,
                    LocalDate deadline, String notes, String applicationUrl) {
        this.id = id;
        this.name = name;
        this.roleTitle = roleTitle;
        this.stage = stage;
        this.deadline = deadline;
        this.notes = notes;
        this.applicationUrl = applicationUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getApplicationUrl() { return applicationUrl; }
    public void setApplicationUrl(String applicationUrl) { this.applicationUrl = applicationUrl; }

    // True if the deadline is today, tomorrow, or within the next 3 days.
    // I'll use this later to highlight urgent rows in the table.
    public boolean isDeadlineSoon() {
        if (deadline == null) return false;
        long daysUntil = LocalDate.now().until(deadline).getDays();
        return daysUntil >= 0 && daysUntil <= 3;
    }

    @Override
    public String toString() {
        return name + " - " + roleTitle + " [" + stage + "]";
    }
}
