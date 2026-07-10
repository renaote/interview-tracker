package com.renate.tracker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.renate.tracker.dao.CompanyDAO;
import com.renate.tracker.model.Company;
import com.renate.tracker.model.Stage;
import com.renate.tracker.util.DatabaseManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// This class connects the FXML layout to the actual logic.
// Every button and field in main.fxml is linked to something in here.
public class MainController {

    @FXML private TableView<Company> companyTable;
    @FXML private TableColumn<Company, String> colName;
    @FXML private TableColumn<Company, String> colRole;
    @FXML private TableColumn<Company, Stage> colStage;
    @FXML private TableColumn<Company, LocalDate> colDeadline;
    @FXML private TableColumn<Company, String> colNotes;

    @FXML private TextField searchField;
    @FXML private ComboBox<Stage> stageFilterBox;
    @FXML private javafx.scene.layout.HBox statCardBox;

    private final DatabaseManager dbManager = new DatabaseManager();
    private final CompanyDAO dao = new CompanyDAO(dbManager);
    private final ObservableList<Company> tableData = FXCollections.observableArrayList();

    // Runs automatically once the FXML has finished loading
    @FXML
    public void initialize() {
        dbManager.initSchema();

        // tells each table column which field on Company to display
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleTitle"));
        colStage.setCellValueFactory(new PropertyValueFactory<>("stage"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // gives rows with a close deadline a light red background
        companyTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Company c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setStyle("");
                } else if (c.isDeadlineSoon()) {
                    setStyle("-fx-background-color: #fdecea;");
                } else {
                    setStyle("");
                }
            }
        });

        companyTable.setItems(tableData);
        stageFilterBox.setItems(FXCollections.observableArrayList(Stage.values()));

        // reload the table automatically whenever the person types or picks a filter
        searchField.textProperty().addListener((obs, oldV, newV) -> refresh());
        stageFilterBox.valueProperty().addListener((obs, oldV, newV) -> refresh());

        refresh();
    }

    // Reloads the table and the stat cards, respecting whatever filter is active
    private void refresh() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        Stage filter = stageFilterBox.getValue();

        List<Company> results = keyword.isEmpty() && filter == null
                ? dao.getAllCompanies()
                : dao.searchCompanies(keyword, filter);

        tableData.setAll(results);
        refreshStatCards();
    }

    private void refreshStatCards() {
        statCardBox.getChildren().clear();
        Map<Stage, Integer> counts = dao.getStageCounts();
        for (Stage s : Stage.values()) {
            statCardBox.getChildren().add(buildStatCard(s.toString(), counts.get(s)));
        }
    }

    // Builds one little box showing a stage name and how many applications are in it
    private VBox buildStatCard(String label, int count) {
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label nameLabel = new Label(label);
        nameLabel.setTextFill(Color.GRAY);
        VBox card = new VBox(2, countLabel, nameLabel);
        card.setStyle("-fx-padding: 8 16 8 16; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
        return card;
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        stageFilterBox.setValue(null);
    }

    @FXML
    private void onAddCompany() {
        showCompanyDialog(null);
    }

    @FXML
    private void onEditCompany() {
        Company selected = companyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a row first", "Pick a company to edit.");
            return;
        }
        showCompanyDialog(selected);
    }

    @FXML
    private void onDeleteCompany() {
        Company selected = companyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a row first", "Pick a company to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + "? This can't be undone.");
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            dao.deleteCompany(selected.getId());
            refresh();
        });
    }

    @FXML
    private void onAdvanceStage() {
        Company selected = companyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a row first", "Pick a company to move forward.");
            return;
        }
        Stage[] stages = Stage.values();
        int next = selected.getStage().ordinal() + 1;
        // stop before REJECTED so this button never accidentally rejects someone -
        // rejecting should be a deliberate choice, made through the edit dialog
        if (next < stages.length && stages[next] != Stage.REJECTED) {
            selected.setStage(stages[next]);
            dao.updateCompany(selected);
            refresh();
        }
    }

    // One popup form used for both adding a new company and editing an existing one
    private void showCompanyDialog(Company existing) {
        Dialog<Company> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add company" : "Edit company");

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField roleField = new TextField(existing != null ? existing.getRoleTitle() : "");
        ComboBox<Stage> stageBox = new ComboBox<>(FXCollections.observableArrayList(Stage.values()));
        stageBox.setValue(existing != null ? existing.getStage() : Stage.APPLIED);
        DatePicker deadlinePicker = new DatePicker(existing != null ? existing.getDeadline() : null);
        TextArea notesArea = new TextArea(existing != null ? existing.getNotes() : "");
        notesArea.setPrefRowCount(3);

        VBox content = new VBox(8,
                new Label("Company name"), nameField,
                new Label("Role"), roleField,
                new Label("Stage"), stageBox,
                new Label("Deadline"), deadlinePicker,
                new Label("Notes"), notesArea);
        content.setStyle("-fx-padding: 12;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            Company c = existing != null ? existing : new Company();
            c.setName(nameField.getText());
            c.setRoleTitle(roleField.getText());
            c.setStage(stageBox.getValue());
            c.setDeadline(deadlinePicker.getValue());
            c.setNotes(notesArea.getText());
            return c;
        });

        Optional<Company> result = dialog.showAndWait();
        result.ifPresent(c -> {
            if (existing == null) {
                dao.addCompany(c);
            } else {
                dao.updateCompany(c);
            }
            refresh();
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.showAndWait();
    }
}