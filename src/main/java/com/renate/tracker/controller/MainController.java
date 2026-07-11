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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
    @FXML private HBox statCardBox;

    private final DatabaseManager dbManager = new DatabaseManager();
    private final CompanyDAO dao = new CompanyDAO(dbManager);
    private final ObservableList<Company> tableData = FXCollections.observableArrayList();

    private static final String[] ACCENT_CLASSES = {
            "accent-applied", "accent-assessment", "accent-interview", "accent-offer", "accent-rejected"
    };

    @FXML
    public void initialize() {
        dbManager.initSchema();

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleTitle"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        colStage.setCellValueFactory(new PropertyValueFactory<>("stage"));
        colStage.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Stage stage, boolean empty) {
                super.updateItem(stage, empty);
                if (empty || stage == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(stage.toString());
                badge.getStyleClass().add("stage-badge");
                badge.getStyleClass().add(switch (stage) {
                    case APPLIED -> "stage-applied";
                    case ASSESSMENT -> "stage-assessment";
                    case INTERVIEW -> "stage-interview";
                    case OFFER -> "stage-offer";
                    case REJECTED -> "stage-rejected";
                });
                setGraphic(badge);
                setText(null);
            }
        });

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

        VBox placeholder = new VBox(
                labelWithClass("No applications yet", "empty-table-title"),
                labelWithClass("Click \"+ Add company\" above to track your first one", "empty-table-subtitle")
        );
        placeholder.getStyleClass().add("empty-table-placeholder");
        companyTable.setPlaceholder(placeholder);

        companyTable.setItems(tableData);
        companyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        stageFilterBox.setItems(FXCollections.observableArrayList(Stage.values()));

        searchField.textProperty().addListener((obs, oldV, newV) -> refresh());
        stageFilterBox.valueProperty().addListener((obs, oldV, newV) -> refresh());

        refresh();
    }

    private Label labelWithClass(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private void refresh() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        Stage filter = stageFilterBox.getValue();

        boolean isFiltering = !keyword.isEmpty() || filter != null;
        if (companyTable.getPlaceholder() instanceof VBox placeholderBox
                && !placeholderBox.getChildren().isEmpty()
                && placeholderBox.getChildren().get(0) instanceof Label titleLabel) {
            titleLabel.setText(isFiltering ? "No matches found" : "No applications yet");
        }

        List<Company> results = keyword.isEmpty() && filter == null
                ? dao.getAllCompanies()
                : dao.searchCompanies(keyword, filter);

        tableData.setAll(results);
        refreshStatCards();
    }

    private void refreshStatCards() {
        statCardBox.getChildren().clear();
        Map<Stage, Integer> counts = dao.getStageCounts();
        Stage[] stages = Stage.values();
        for (int i = 0; i < stages.length; i++) {
            statCardBox.getChildren().add(
                    buildStatCard(stages[i].toString(), counts.get(stages[i]), ACCENT_CLASSES[i]));
        }
    }

    private VBox buildStatCard(String label, int count, String accentClass) {
        Region accent = new Region();
        accent.getStyleClass().add(accentClass);
        accent.setMaxWidth(Double.MAX_VALUE);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("stat-count");
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");

        VBox textBox = new VBox(2, countLabel, nameLabel);
        textBox.setStyle("-fx-alignment: center; -fx-padding: 10 18 10 18;");

        VBox card = new VBox(accent, textBox);
        card.getStyleClass().add("stat-card");
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
                "Are you sure you want to delete " + selected.getName() + "? This action can't be undone.");
        confirm.setTitle("Delete company");
        confirm.setHeaderText(null);
        styleDialog(confirm.getDialogPane());
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
        if (next < stages.length && stages[next] != Stage.REJECTED) {
            selected.setStage(stages[next]);
            dao.updateCompany(selected);
            refresh();
        }
    }

    // One popup form used for both adding a new company and editing an existing one.
    // Validates each field separately (name, role, deadline) so the person sees
    // exactly which field needs fixing instead of one vague combined message.
    private void showCompanyDialog(Company existing) {
        Dialog<Company> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add company" : "Edit company");
        styleDialog(dialog.getDialogPane());

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField roleField = new TextField(existing != null ? existing.getRoleTitle() : "");
        ComboBox<Stage> stageBox = new ComboBox<>(FXCollections.observableArrayList(Stage.values()));
        stageBox.setValue(existing != null ? existing.getStage() : Stage.APPLIED);

        DatePicker deadlinePicker = new DatePicker(existing != null ? existing.getDeadline() : null);
        // Gray out and disable any date before today, so a mistaken past
        // deadline can't even be selected in the first place
        deadlinePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #9ca3af;");
                }
            }
        });

        TextArea notesArea = new TextArea(existing != null ? existing.getNotes() : "");
        notesArea.setPrefRowCount(3);

        Label nameError = new Label();
        nameError.getStyleClass().add("error-label");
        Label roleError = new Label();
        roleError.getStyleClass().add("error-label");
        Label deadlineError = new Label();
        deadlineError.getStyleClass().add("error-label");

        VBox content = new VBox(4,
                labelWithClass("Company name", "field-label"), nameField, nameError,
                labelWithClass("Role", "field-label"), roleField, roleError,
                labelWithClass("Stage", "field-label"), stageBox,
                labelWithClass("Deadline", "field-label"), deadlinePicker, deadlineError,
                labelWithClass("Notes", "field-label"), notesArea);
        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            nameField.getStyleClass().remove("field-invalid");
            roleField.getStyleClass().remove("field-invalid");
            deadlinePicker.getStyleClass().remove("field-invalid");
            nameError.setText("");
            roleError.setText("");
            deadlineError.setText("");

            boolean valid = true;

            if (nameField.getText() == null || nameField.getText().isBlank()) {
                nameField.getStyleClass().add("field-invalid");
                nameError.setText("Company name is required.");
                valid = false;
            }
            if (roleField.getText() == null || roleField.getText().isBlank()) {
                roleField.getStyleClass().add("field-invalid");
                roleError.setText("Role is required.");
                valid = false;
            }
            LocalDate deadline = deadlinePicker.getValue();
            if (deadline != null && deadline.isBefore(LocalDate.now())) {
                deadlinePicker.getStyleClass().add("field-invalid");
                deadlineError.setText("Deadline can't be in the past.");
                valid = false;
            }

            if (!valid) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            Company c = existing != null ? existing : new Company();
            c.setName(nameField.getText().trim());
            c.setRoleTitle(roleField.getText().trim());
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

    // Attaches our stylesheet, sets the app icon on the dialog's actual
    // window (the OS title bar), and puts the same icon in the blue header
    // banner too - dialogs are separate windows, so none of this happens
    // automatically the way it does for the main window.
    //
    // Note: uses the full "javafx.stage.Stage" name here instead of a plain
    // import, because we already have our own Stage enum (Applied/Interview/
    // etc.) and Java can't tell the two apart if both are imported at once.
    private void styleDialog(DialogPane pane) {
        pane.getStylesheets().add(
                getClass().getResource("/com/renate/tracker/view/styles.css").toExternalForm());

        Image appIcon = new Image(getClass().getResourceAsStream("/images/app-icon.png"));

        ImageView headerIcon = new ImageView(appIcon);
        headerIcon.setFitWidth(28);
        headerIcon.setFitHeight(28);
        pane.setGraphic(headerIcon);

        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof javafx.stage.Stage windowStage) {
                windowStage.getIcons().add(appIcon);
            }
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }
}