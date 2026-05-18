package edu.ccrm.gui;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.*;
import edu.ccrm.io.BackupService;
import edu.ccrm.io.DatabaseInitializer;
import edu.ccrm.io.ImportExportService;
import edu.ccrm.service.*;
import edu.ccrm.util.RecursiveUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private final StudentService studentService = new StudentService();
    private final InstructorService instructorService = new InstructorService();
    private final CourseService courseService = new CourseService(instructorService);
    private final EnrollmentService enrollmentService = new EnrollmentService(studentService, courseService);
    private final TranscriptService transcriptService = new TranscriptService(studentService, enrollmentService);
    private final ImportExportService importExportService = new ImportExportService(studentService, instructorService, courseService, enrollmentService);
    private final BackupService backupService = new BackupService();
    private final DatabaseAdminService dbAdminService = new DatabaseAdminService();

    private ProgressIndicator globalProgressIndicator;
    private Label globalStatusLabel;
    private Region dimOverlay;
    private ProgressIndicator centerSpinner;
    private ProgressBar centerProgressBar;
    private Label progressTextLabel;
    private ProgressBar recordProgressBar;
    private Label recordProgressLabel;
    private VBox progressBox;

    public static void main(String[] args) {
        launch(args);
    }

    private BorderPane mainLayout;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        primaryStage.setTitle("Campus Course & Records Manager (CCRM)");

        globalProgressIndicator = new ProgressIndicator();
        globalProgressIndicator.setVisible(false);
        globalProgressIndicator.setPrefSize(20, 20);
        globalStatusLabel = new Label("Ready");

        dimOverlay = new Region();
        dimOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        dimOverlay.setVisible(false);
        
        centerSpinner = new ProgressIndicator();
        centerSpinner.setMaxSize(60, 60);

        centerProgressBar = new ProgressBar(0);
        centerProgressBar.setPrefWidth(200);
        
        progressTextLabel = new Label();
        progressTextLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        recordProgressBar = new ProgressBar(0);
        recordProgressBar.setPrefWidth(200);
        recordProgressBar.setVisible(false);

        recordProgressLabel = new Label();
        recordProgressLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
        recordProgressLabel.setVisible(false);

        Label progressHeading = new Label("Progress");
        progressHeading.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        progressBox = new VBox(10, progressHeading, centerSpinner, centerProgressBar, progressTextLabel, recordProgressBar, recordProgressLabel);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMaxSize(400, 230);
        progressBox.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 25; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: rgba(255,255,255,0.2); -fx-border-width: 1;");
        progressBox.setVisible(false);

        ToggleButton themeToggle = new ToggleButton();
        themeToggle.setGraphic(new FontIcon("fas-moon"));
        themeToggle.setOnAction(e -> {
            if (themeToggle.isSelected()) {
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                themeToggle.setGraphic(new FontIcon("fas-sun"));
            } else {
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                themeToggle.setGraphic(new FontIcon("fas-moon"));
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(10, globalProgressIndicator, globalStatusLabel, spacer, themeToggle);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        mainLayout = new BorderPane();
        mainLayout.setBottom(statusBar);

        // Sidebar
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 0 1px 0 0;");

        Label brandLabel = new Label("CCRM Console");
        brandLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        sidebar.getChildren().add(brandLabel);
        
        Button btnDashboard = createNavButton("Dashboard Overview", "fas-chart-pie");
        Button btnStudents = createNavButton("Manage Students", "fas-user-graduate");
        Button btnInstructors = createNavButton("Manage Faculty", "fas-chalkboard-teacher");
        Button btnCourses = createNavButton("Course Registry", "fas-book");
        Button btnEnrollments = createNavButton("Enrollments & Grades", "fas-id-card");
        Button btnSystem = createNavButton("System & Backup Ops", "fas-database");

        sidebar.getChildren().addAll(new Separator(), btnDashboard, btnStudents, btnInstructors, btnCourses, btnEnrollments, btnSystem);
        mainLayout.setLeft(sidebar);

        btnDashboard.setOnAction(e -> mainLayout.setCenter(createDashboardView()));
        btnStudents.setOnAction(e -> mainLayout.setCenter(createStudentsView()));
        btnInstructors.setOnAction(e -> mainLayout.setCenter(createInstructorsView()));
        btnCourses.setOnAction(e -> mainLayout.setCenter(createCoursesView()));
        btnEnrollments.setOnAction(e -> mainLayout.setCenter(createEnrollmentsView()));
        btnSystem.setOnAction(e -> mainLayout.setCenter(createFileOperationsView()));

        mainLayout.setCenter(createDashboardView());

        StackPane appRoot = new StackPane();
        appRoot.getChildren().addAll(mainLayout, dimOverlay, progressBox);

        Scene scene = new Scene(appRoot, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize Database in background
        runTaskWithProgress("Initializing Database...", () -> {
            DatabaseInitializer.initialize();
        }, () -> {
            // After init, check if this is a fresh install
            if (DatabaseInitializer.isFirstRun()) {
                Alert firstRunAlert = new Alert(Alert.AlertType.CONFIRMATION);
                firstRunAlert.setTitle("Fresh Database Detected");
                firstRunAlert.setHeaderText("Welcome to CCRM!");
                firstRunAlert.setContentText(
                    "A new database has been created.\n\n" +
                    "Would you like to load the bundled sample data to get started quickly?\n\n" +
                    "Click OK to load sample data, or Cancel to start with an empty database and import your own files."
                );
                firstRunAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        runTaskWithProgress("Loading sample data...",
                            DatabaseInitializer::importSampleData,
                            () -> showAlert(Alert.AlertType.INFORMATION, "Sample data loaded successfully!"));
                    } else {
                        showAlert(Alert.AlertType.INFORMATION,
                            "Empty database ready. Use the 'System & Backup Ops' panel to import your own CSV files.");
                    }
                });
            }
        });
    }

    private Button createNavButton(String text, String iconCode) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        Button button = new Button(text, icon);
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("flat");
        return button;
    }

    private javafx.scene.Node createDashboardView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        
        Label title = new Label("Dashboard Overview");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        java.util.List<Student> allStudents = studentService.getAllStudentsSortedById();
        int active = 0, prob = 0, susp = 0, graduated = 0;
        for (Student s : allStudents) {
            if (s.getStatus() == Student.Status.ACTIVE) active++;
            else if (s.getStatus() == Student.Status.PROBATION) prob++;
            else if (s.getStatus() == Student.Status.INACTIVE) susp++;
            else if (s.getStatus() == Student.Status.GRADUATED) graduated++;
        }

        HBox cardsBox = new HBox(20);
        cardsBox.getChildren().addAll(
            createMetricCard("Total Students", String.valueOf(allStudents.size()), "fas-users"),
            createMetricCard("Active Courses", String.valueOf(courseService.getAllCoursesSortedByCode().size()), "fas-book-open"),
            createMetricCard("Total Faculty", String.valueOf(instructorService.getAllInstructorsSortedById().size()), "fas-chalkboard-teacher"),
            createMetricCard("Graduated", String.valueOf(graduated), "fas-graduation-cap")
        );
        
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.BarChart<String,Number> bc = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        bc.setTitle("Students by Status");
        bc.setLegendVisible(false);
        javafx.scene.chart.XYChart.Series<String,Number> series1 = new javafx.scene.chart.XYChart.Series<>();
        
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("ACTIVE", active));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("PROBATION", prob));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("INACTIVE", susp));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("GRADUATED", graduated));
        bc.getData().add(series1);
        
        layout.getChildren().addAll(title, cardsBox, bc);
        return layout;
    }

    private VBox createMetricCard(String title, String value, String iconCode) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefSize(200, 100);
        card.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 14px;");
        
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setStyle("-fx-text-fill: -color-accent-fg;");
        
        BorderPane inner = new BorderPane();
        inner.setLeft(lblValue);
        inner.setRight(icon);
        
        card.getChildren().addAll(lblTitle, inner);
        return card;
    }

    private void runTaskWithProgress(String message, TaskAction action, Runnable onSuccess) {
        globalProgressIndicator.setVisible(true);
        globalStatusLabel.setText(message);
        dimOverlay.setVisible(true);
        progressBox.setVisible(true);
        centerSpinner.setVisible(true);
        centerProgressBar.setVisible(false);
        progressTextLabel.setVisible(true);
        progressTextLabel.textProperty().unbind();
        progressTextLabel.setText(message);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.execute();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            globalProgressIndicator.setVisible(false);
            dimOverlay.setVisible(false);
            progressBox.setVisible(false);
            globalStatusLabel.setText("Ready");
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            globalProgressIndicator.setVisible(false);
            dimOverlay.setVisible(false);
            progressBox.setVisible(false);
            globalStatusLabel.setText("Error occurred");
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        new Thread(task).start();
    }
    
    private boolean verifyHeader(Path file, String[] requiredFields) {
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(file)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                return false;
            }
            String[] headers = headerLine.split(",");
            java.util.Set<String> headerSet = new java.util.HashSet<>();
            for (String h : headers) {
                headerSet.add(h.trim().toLowerCase().replaceAll("\"", ""));
            }
            for (String req : requiredFields) {
                if (!headerSet.contains(req.toLowerCase())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    interface FileImportAction {
        void execute(Path path, ImportExportService.ImportProgressCallback callback) throws Exception;
    }

    private void handleSingleImport(String moduleName, String[] requiredFields, FileImportAction importAction) {
        Alert prompt = new Alert(Alert.AlertType.INFORMATION);
        prompt.setTitle("File Selection");
        prompt.setHeaderText("Import " + moduleName);
        prompt.setContentText("You will now be prompted to select the CSV file containing " + moduleName + " data.\n\nClick OK to proceed.");
        prompt.showAndWait();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select " + moduleName + " CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(mainLayout.getScene().getWindow());
        if (file != null) {
            if (!verifyHeader(file.toPath(), requiredFields)) {
                showAlert(Alert.AlertType.ERROR, "Invalid CSV format for " + moduleName + ".\nExpected header fields (in any order):\n" + String.join(", ", requiredFields));
                return;
            }
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("Importing " + moduleName + "...");
                    updateProgress(0, 1);
                    importAction.execute(file.toPath(), (processed, total) -> {
                        updateProgress(processed, total);
                        int pct = total > 0 ? (int)((processed * 100L) / total) : 0;
                        updateMessage(String.format("Importing %s: Record %d / %d (%d%%)", moduleName, processed, total, pct));
                    });
                    return null;
                }
            };
            runJavaFXTask(task, "Importing " + moduleName + "...", () -> showAlert(Alert.AlertType.INFORMATION, moduleName + " imported successfully."));
        }
    }

    private void runJavaFXTask(Task<?> task, String initialMessage, Runnable onSuccess) {
        globalProgressIndicator.setVisible(true);
        globalStatusLabel.setText(initialMessage);
        dimOverlay.setVisible(true);
        progressBox.setVisible(true);
        centerSpinner.setVisible(false);
        centerProgressBar.setVisible(true);
        progressTextLabel.setVisible(true);
        // Hide record-level bar by default; handleImportAll shows it explicitly
        recordProgressBar.setVisible(false);
        recordProgressLabel.setVisible(false);
        
        centerProgressBar.progressProperty().bind(task.progressProperty());
        progressTextLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            centerProgressBar.progressProperty().unbind();
            progressTextLabel.textProperty().unbind();
            recordProgressBar.setVisible(false);
            recordProgressLabel.setVisible(false);
            globalProgressIndicator.setVisible(false);
            dimOverlay.setVisible(false);
            progressBox.setVisible(false);
            globalStatusLabel.setText("Ready");
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            centerProgressBar.progressProperty().unbind();
            progressTextLabel.textProperty().unbind();
            recordProgressBar.setVisible(false);
            recordProgressLabel.setVisible(false);
            globalProgressIndicator.setVisible(false);
            dimOverlay.setVisible(false);
            progressBox.setVisible(false);
            globalStatusLabel.setText("Error occurred");
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        new Thread(task).start();
    }
    
    private void handleImportAll() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        Alert prompt = new Alert(Alert.AlertType.INFORMATION);
        prompt.setTitle("Bulk Import Sequence");
        prompt.setHeaderText("Bulk Import Started");
        prompt.setContentText("You are about to bulk import all data. You will be prompted to select 4 files in the exact order needed by the database:\n\n1. Instructors\n2. Courses\n3. Students\n4. Enrollments\n\nClick OK to select the first file (Instructors).");
        prompt.showAndWait();

        String[] instReq = {"FiD", "firstName", "lastName", "email", "department", "dob", "phone", "cabinNo"};
        String[] coursesReq = {"code", "title", "credits", "department", "instructorId", "semester", "classroomNo"};
        String[] studentsReq = {"id", "regNo", "firstName", "lastName", "email", "status", "registrationDate", "dob", "phone"};
        String[] enrollReq = {"studentRegNo", "courseCode", "grade"};

        chooser.setTitle("1. Select Instructors CSV File");
        File instFile = chooser.showOpenDialog(mainLayout.getScene().getWindow());
        if (instFile == null) return;
        if (!verifyHeader(instFile.toPath(), instReq)) {
            showAlert(Alert.AlertType.ERROR, "Invalid CSV format for Instructors.\nExpected header fields:\n" + String.join(", ", instReq));
            return;
        }
        
        prompt.setHeaderText("Next: Courses");
        prompt.setContentText("Instructors selected successfully.\n\nClick OK to select the second file (Courses).");
        prompt.showAndWait();

        chooser.setTitle("2. Select Courses CSV File");
        File coursesFile = chooser.showOpenDialog(mainLayout.getScene().getWindow());
        if (coursesFile == null) return;
        if (!verifyHeader(coursesFile.toPath(), coursesReq)) {
            showAlert(Alert.AlertType.ERROR, "Invalid CSV format for Courses.\nExpected header fields:\n" + String.join(", ", coursesReq));
            return;
        }
        
        prompt.setHeaderText("Next: Students");
        prompt.setContentText("Courses selected successfully.\n\nClick OK to select the third file (Students).");
        prompt.showAndWait();

        chooser.setTitle("3. Select Students CSV File");
        File studentsFile = chooser.showOpenDialog(mainLayout.getScene().getWindow());
        if (studentsFile == null) return;
        if (!verifyHeader(studentsFile.toPath(), studentsReq)) {
            showAlert(Alert.AlertType.ERROR, "Invalid CSV format for Students.\nExpected header fields:\n" + String.join(", ", studentsReq));
            return;
        }
        
        prompt.setHeaderText("Next: Enrollments");
        prompt.setContentText("Students selected successfully.\n\nClick OK to select the final file (Enrollments).");
        prompt.showAndWait();

        chooser.setTitle("4. Select Enrollments CSV File");
        File enrollFile = chooser.showOpenDialog(mainLayout.getScene().getWindow());
        if (enrollFile == null) return;
        if (!verifyHeader(enrollFile.toPath(), enrollReq)) {
            showAlert(Alert.AlertType.ERROR, "Invalid CSV format for Enrollments.\nExpected header fields:\n" + String.join(", ", enrollReq));
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 4);
                
                Platform.runLater(() -> {
                    recordProgressBar.setVisible(true);
                    recordProgressLabel.setVisible(true);
                });

                updateMessage("Importing Instructors (1/4)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importInstructorsFile(instFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(1, 4);
                Thread.sleep(300);

                updateMessage("Importing Courses (2/4)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importCoursesFile(coursesFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(2, 4);
                Thread.sleep(300);

                updateMessage("Importing Students (3/4)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importStudentsFile(studentsFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(3, 4);
                Thread.sleep(300);

                updateMessage("Importing Enrollments (4/4)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importEnrollmentsFile(enrollFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(4, 4);
                Thread.sleep(300);
                
                return null;
            }
        };

        runJavaFXTask(task, "Starting Bulk Import...", () -> {
            showAlert(Alert.AlertType.INFORMATION, "All data imported successfully in the required sequence without corruption.");
        });
    }

    interface TaskAction {
        void execute() throws Exception;
    }

    private void showAlert(Alert.AlertType type, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // =========================================================================
    // STUDENTS TAB
    // =========================================================================
    private javafx.scene.Node createStudentsView() {

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        // Input Form
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        
        TextField idField = new TextField();
        TextField regNoField = new TextField();
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField emailField = new TextField();
        DatePicker dobPicker = new DatePicker();
        TextField phoneField = new TextField();
        DatePicker regDatePicker = new DatePicker();

        form.addRow(0, new Label("Student ID:"), idField);
        form.addRow(1, new Label("Reg No:"), regNoField);
        form.addRow(2, new Label("First Name:"), firstNameField);
        form.addRow(3, new Label("Last Name:"), lastNameField);
        form.addRow(4, new Label("Email:"), emailField);
        form.addRow(5, new Label("Phone:"), phoneField);
        form.addRow(6, new Label("DOB:"), dobPicker);
        form.addRow(7, new Label("Reg Date:"), regDatePicker);

        Button btnAdd = new Button("Add Student", new FontIcon("fas-user-plus"));
        form.add(btnAdd, 0, 8, 2, 1);
        
        TitledPane formPane = new TitledPane("Add New Student", form);
        formPane.setCollapsible(false);
        VBox leftPane = new VBox(formPane);
        leftPane.setPadding(new Insets(10));

        // Table
        TableView<Student> table = new TableView<>();
        TableColumn<Student, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));

        TableColumn<Student, String> colRegNo = new TableColumn<>("Reg No");
        colRegNo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRegNo()));

        TableColumn<Student, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName().toString()));

        TableColumn<Student, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
        
        TableColumn<Student, String> colDob = new TableColumn<>("DOB");
        colDob.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDob() != null ? c.getValue().getDob().toString() : ""));

        TableColumn<Student, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPhone()));

        TableColumn<Student, String> colRegDate = new TableColumn<>("Reg Date");
        colRegDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRegistrationDate() != null ? c.getValue().getRegistrationDate().toString() : ""));

        TableColumn<Student, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().toString()));
        colStatus.setCellFactory(column -> new TableCell<Student, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.setPadding(new Insets(2, 8, 2, 8));
                    badge.setStyle("-fx-background-radius: 10; -fx-font-weight: bold;");
                    if ("ACTIVE".equals(item)) {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-success-fg; -fx-background-color: -color-success-muted;");
                    } else if ("PROBATION".equals(item)) {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-warning-fg; -fx-background-color: -color-warning-muted;");
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-danger-fg; -fx-background-color: -color-danger-muted;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colId, colRegNo, colName, colEmail, colDob, colPhone, colRegDate, colStatus);
        table.setPrefHeight(650);

        ObservableList<Student> masterData = FXCollections.observableArrayList();
        FilteredList<Student> filteredData = new FilteredList<>(masterData, p -> true);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name or Reg No...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(student -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (student.getFullName().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                if (student.getRegNo().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });
        
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        Runnable refreshTable = () -> {
            runTaskWithProgress("Loading Students...", () -> {
                List<Student> students = studentService.getAllStudentsSortedById();
                Platform.runLater(() -> masterData.setAll(students));
            }, null);
        };

        btnAdd.setOnAction(e -> {
            try {
                int id = Integer.parseInt(idField.getText());
                java.time.LocalDate regDate = regDatePicker.getValue() != null ? regDatePicker.getValue() : java.time.LocalDate.now();
                Student s = new Student(id, regNoField.getText(), new Name(firstNameField.getText(), lastNameField.getText()), emailField.getText(), Student.Status.ACTIVE, regDate, dobPicker.getValue(), phoneField.getText());
                studentService.addStudent(s);
                showAlert(Alert.AlertType.INFORMATION, "Student added successfully.");
                refreshTable.run();
                idField.clear(); regNoField.clear(); firstNameField.clear(); lastNameField.clear(); emailField.clear(); phoneField.clear(); dobPicker.setValue(null); regDatePicker.setValue(null);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to add student: " + ex.getMessage());
            }
        });

        // Bottom Actions
        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.setOnAction(e -> refreshTable.run());

        java.util.function.Consumer<Student> transcriptAction = selected -> {
            runTaskWithProgress("Generating Transcript...", () -> {
                try {
                    List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(selected.getRegNo());
                    double cgpa = transcriptService.calculateCGPA(enrollments);
                    
                    Platform.runLater(() -> {
                        Dialog<Void> dialog = new Dialog<>();
                        dialog.setTitle("Transcript");
                        dialog.setHeaderText("Transcript for " + selected.getFullName());

                        VBox content = new VBox(15);
                        content.setPadding(new Insets(10));
                        
                        GridPane headerInfo = new GridPane();
                        headerInfo.setHgap(15); headerInfo.setVgap(5);
                        headerInfo.addRow(0, new Label("Student ID:"), new Label(String.valueOf(selected.getId())));
                        headerInfo.addRow(1, new Label("Registration No:"), new Label(selected.getRegNo()));
                        headerInfo.addRow(2, new Label("Status:"), new Label(selected.getStatus().toString()));
                        
                        TableView<Enrollment> coursesTable = new TableView<>();
                        
                        TableColumn<Enrollment, String> cCode = new TableColumn<>("Code");
                        cCode.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getCourseCode().getCode()));
                        TableColumn<Enrollment, String> cTitle = new TableColumn<>("Title");
                        cTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getTitle()));
                        TableColumn<Enrollment, Integer> cCredits = new TableColumn<>("Credits");
                        cCredits.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCourse().getCredits()));
                        TableColumn<Enrollment, String> cSemester = new TableColumn<>("Semester");
                        cSemester.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                            c.getValue().getEnrollmentSemester() != null && !c.getValue().getEnrollmentSemester().isEmpty()
                                ? c.getValue().getEnrollmentSemester()
                                : (c.getValue().getCourse().getSemester() != null ? c.getValue().getCourse().getSemester().name() : "N/A")));
                        TableColumn<Enrollment, String> cYear = new TableColumn<>("Year");
                        cYear.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                            c.getValue().getEnrollmentYear() > 0
                                ? String.valueOf(c.getValue().getEnrollmentYear())
                                : "N/A"));
                        TableColumn<Enrollment, String> cGrade = new TableColumn<>("Grade");
                        cGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade() != null ? c.getValue().getGrade().toString() : "N/A"));
                        
                        coursesTable.getColumns().addAll(cCode, cTitle, cCredits, cSemester, cYear, cGrade);
                        coursesTable.getItems().setAll(enrollments);
                        coursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                        VBox.setVgrow(coursesTable, Priority.ALWAYS);

                        Label cgpaLabel = new Label(String.format("CGPA: %.2f", cgpa));
                        cgpaLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        cgpaLabel.setStyle("-fx-text-fill: white;");

                        content.getChildren().addAll(headerInfo, coursesTable, cgpaLabel);
                        VBox.setVgrow(coursesTable, Priority.ALWAYS);

                        content.prefWidthProperty().bind(dialog.getDialogPane().widthProperty().subtract(40));
                        content.prefHeightProperty().bind(dialog.getDialogPane().heightProperty().subtract(140));

                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.setResizable(true);
                        dialog.getDialogPane().setPrefSize(950, 650);
                        dialog.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Failed to load transcript: " + ex.getMessage()));
                }
            }, null);
        };

        java.util.function.Consumer<Student> updateStatusAction = selected -> {
            ChoiceDialog<Student.Status> dialog = new ChoiceDialog<>(selected.getStatus(), Student.Status.values());
            dialog.setTitle("Update Status");
            dialog.setHeaderText("Select new status for " + selected.getRegNo());
            dialog.showAndWait().ifPresent(status -> {
                try {
                    studentService.updateStudentStatus(selected.getRegNo(), status);
                    refreshTable.run();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Failed to update status: " + ex.getMessage());
                }
            });
        };

        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem viewTranscript = new MenuItem("View Transcript");
            viewTranscript.setGraphic(new FontIcon("fas-file-invoice"));
            viewTranscript.setOnAction(e -> {
                Student selected = row.getItem();
                if (selected != null) {
                    transcriptAction.accept(selected);
                }
            });
            
            MenuItem changeStatus = new MenuItem("Change Status");
            changeStatus.setGraphic(new FontIcon("fas-edit"));
            changeStatus.setOnAction(e -> {
                Student selected = row.getItem();
                if (selected != null) {
                    updateStatusAction.accept(selected);
                }
            });
            
            contextMenu.getItems().addAll(viewTranscript, changeStatus);
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            return row;
        });

        HBox topBox = new HBox(10, new Label("Search:"), searchField, btnRefresh);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        
        VBox rightPane = new VBox(topBox, table);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(rightPane, leftPane);
        splitPane.setDividerPositions(0.70);

        layout.setCenter(splitPane);

        
        // Refresh on load
        refreshTable.run();
        return layout;
    }

    // =========================================================================
    // INSTRUCTORS TAB
    // =========================================================================
    private javafx.scene.Node createInstructorsView() {

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        // Input Form
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        
        TextField fidField = new TextField();
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField emailField = new TextField();
        TextField deptField = new TextField();
        DatePicker dobPicker = new DatePicker();
        TextField phoneField = new TextField();
        TextField cabinField = new TextField();

        form.addRow(0, new Label("Instructor FiD:"), fidField);
        form.addRow(1, new Label("Department:"), deptField);
        form.addRow(2, new Label("First Name:"), firstNameField);
        form.addRow(3, new Label("Last Name:"), lastNameField);
        form.addRow(4, new Label("Email:"), emailField);
        form.addRow(5, new Label("Phone:"), phoneField);
        form.addRow(6, new Label("DOB:"), dobPicker);
        form.addRow(7, new Label("Cabin No:"), cabinField);

        Button btnAdd = new Button("Add Instructor", new FontIcon("fas-user-plus"));
        form.add(btnAdd, 0, 8, 2, 1);
        
        TitledPane formPane = new TitledPane("Add New Instructor", form);
        formPane.setCollapsible(false);
        VBox leftPane = new VBox(formPane);
        leftPane.setPadding(new Insets(10));

        // Table
        TableView<Instructor> table = new TableView<>();
        TableColumn<Instructor, String> colFid = new TableColumn<>("FiD");
        colFid.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFiD()));

        TableColumn<Instructor, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName().toString()));

        TableColumn<Instructor, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));

        TableColumn<Instructor, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDepartment()));
        
        TableColumn<Instructor, String> colDob = new TableColumn<>("DOB");
        colDob.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDob() != null ? c.getValue().getDob().toString() : ""));

        TableColumn<Instructor, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPhone()));
        
        TableColumn<Instructor, String> colCabin = new TableColumn<>("Cabin");
        colCabin.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCabinNo()));

        table.getColumns().addAll(colFid, colName, colEmail, colDept, colDob, colPhone, colCabin);
        table.setPrefHeight(650);

        ObservableList<Instructor> masterData = FXCollections.observableArrayList();
        FilteredList<Instructor> filteredData = new FilteredList<>(masterData, p -> true);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name or Dept...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(inst -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (inst.getFullName().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                if (inst.getDepartment().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        SortedList<Instructor> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        Runnable refreshTable = () -> {
            runTaskWithProgress("Loading Instructors...", () -> {
                List<Instructor> list = instructorService.getAllInstructorsSortedById();
                Platform.runLater(() -> masterData.setAll(list));
            }, null);
        };

        btnAdd.setOnAction(e -> {
            try {
                Instructor inst = new Instructor(fidField.getText(), new Name(firstNameField.getText(), lastNameField.getText()), emailField.getText(), deptField.getText(), dobPicker.getValue(), phoneField.getText(), cabinField.getText());
                instructorService.addInstructor(inst);
                showAlert(Alert.AlertType.INFORMATION, "Instructor added successfully.");
                refreshTable.run();
                fidField.clear(); firstNameField.clear(); lastNameField.clear(); emailField.clear(); deptField.clear(); phoneField.clear(); dobPicker.setValue(null); cabinField.clear();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to add instructor: " + ex.getMessage());
            }
        });

        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.setOnAction(e -> refreshTable.run());

        HBox topBox = new HBox(10, new Label("Search:"), searchField, btnRefresh);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        VBox rightPane = new VBox(topBox, table);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(rightPane, leftPane);
        splitPane.setDividerPositions(0.70);

        layout.setCenter(splitPane);
        
        
        refreshTable.run();
        return layout;
    }

    // =========================================================================
    // COURSES TAB
    // =========================================================================
    private javafx.scene.Node createCoursesView() {

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        // Form
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        
        TextField codeField = new TextField();
        TextField titleField = new TextField();
        TextField creditsField = new TextField();
        TextField deptField = new TextField();
        ComboBox<Semester> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll(Semester.values());
        TextField classroomField = new TextField();

        form.addRow(0, new Label("Code:"), codeField);
        form.addRow(1, new Label("Title:"), titleField);
        form.addRow(2, new Label("Credits:"), creditsField);
        form.addRow(3, new Label("Department:"), deptField);
        form.addRow(4, new Label("Semester:"), semesterCombo);
        form.addRow(5, new Label("Classroom:"), classroomField);

        Button btnAdd = new Button("Add Course", new FontIcon("fas-plus"));
        form.add(btnAdd, 0, 6, 2, 1);
        
        TitledPane formPane = new TitledPane("Add New Course", form);
        formPane.setCollapsible(false);
        VBox leftPane = new VBox(formPane);
        leftPane.setPadding(new Insets(10));

        // Table
        TableView<Course> table = new TableView<>();
        TableColumn<Course, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourseCode().getCode()));
        TableColumn<Course, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTitle()));
        TableColumn<Course, Integer> colCredits = new TableColumn<>("Credits");
        colCredits.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCredits()));
        TableColumn<Course, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDepartment()));
        TableColumn<Course, String> colSem = new TableColumn<>("Semester");
        colSem.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSemester().toString()));
        TableColumn<Course, String> colInst = new TableColumn<>("Instructor");
        colInst.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getInstructor() != null ? c.getValue().getInstructor().getFullName().toString() : "N/A"));
        
        TableColumn<Course, String> colClassroom = new TableColumn<>("Classroom");
        colClassroom.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getClassroomNo() != null ? c.getValue().getClassroomNo() : ""));

        table.getColumns().addAll(colCode, colTitle, colCredits, colDept, colSem, colInst, colClassroom);
        table.setPrefHeight(650);

        ObservableList<Course> masterData = FXCollections.observableArrayList();
        FilteredList<Course> filteredData = new FilteredList<>(masterData, p -> true);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by Code or Title...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(course -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (course.getCourseCode().getCode().toLowerCase().contains(lowerCaseFilter)) return true;
                if (course.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        SortedList<Course> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        Runnable refreshTable = () -> {
            runTaskWithProgress("Loading Courses...", () -> {
                List<Course> list = courseService.getAllCoursesSortedByCode();
                Platform.runLater(() -> masterData.setAll(list));
            }, null);
        };

        btnAdd.setOnAction(e -> {
            try {
                Course course = new Course.Builder(new CourseCode(codeField.getText()))
                        .withTitle(titleField.getText())
                        .withCredits(Integer.parseInt(creditsField.getText()))
                        .withDepartment(deptField.getText())
                        .withSemester(semesterCombo.getValue())
                        .withClassroomNo(classroomField.getText())
                        .build();
                courseService.addCourse(course);
                showAlert(Alert.AlertType.INFORMATION, "Course added successfully.");
                refreshTable.run();
                codeField.clear(); titleField.clear(); creditsField.clear(); deptField.clear(); semesterCombo.setValue(null); classroomField.clear();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to add course: " + ex.getMessage());
            }
        });

        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.setOnAction(e -> refreshTable.run());

        java.util.function.Consumer<Course> assignAction = selected -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Assign Instructor");
            dialog.setHeaderText("Assign to " + selected.getCourseCode().getCode());
            dialog.setContentText("Enter Instructor FiD:");
            dialog.showAndWait().ifPresent(fid -> {
                try {
                    courseService.assignInstructor(selected.getCourseCode(), fid);
                    refreshTable.run();
                } catch(Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Failed to assign: " + ex.getMessage());
                }
            });
        };

        table.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem assignMenu = new MenuItem("Assign Instructor");
            assignMenu.setGraphic(new FontIcon("fas-chalkboard-teacher"));
            assignMenu.setOnAction(e -> {
                Course selected = row.getItem();
                if (selected != null) {
                    assignAction.accept(selected);
                }
            });
            contextMenu.getItems().add(assignMenu);
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            return row;
        });

        HBox topBox = new HBox(10, new Label("Search:"), searchField, btnRefresh);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        VBox rightPane = new VBox(topBox, table);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(rightPane, leftPane);
        splitPane.setDividerPositions(0.70);

        layout.setCenter(splitPane);
        
        
        refreshTable.run();
        return layout;
    }

    // =========================================================================
    // ENROLLMENTS TAB
    // =========================================================================
    private javafx.scene.Node createEnrollmentsView() {

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        // Form
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        
        TextField regNoField = new TextField();
        TextField courseCodeField = new TextField();
        ComboBox<Grade> gradeCombo = new ComboBox<>();
        gradeCombo.getItems().addAll(Grade.values());

        form.addRow(0, new Label("Student Reg No:"), regNoField);
        form.addRow(1, new Label("Course Code:"), courseCodeField);
        form.addRow(2, new Label("Grade (for recording):"), gradeCombo);

        Button btnEnroll = new Button("Enroll", new FontIcon("fas-user-check"));
        Button btnUnenroll = new Button("Unenroll", new FontIcon("fas-user-minus"));
        Button btnRecordGrade = new Button("Record Grade", new FontIcon("fas-pen"));

        javafx.scene.layout.FlowPane formActions = new javafx.scene.layout.FlowPane(10, 10);
        formActions.getChildren().addAll(btnEnroll, btnUnenroll, btnRecordGrade);
        form.add(formActions, 0, 3, 2, 1);

        TitledPane formPane = new TitledPane("Enrollment Actions", form);
        formPane.setCollapsible(false);
        VBox leftPane = new VBox(formPane);
        leftPane.setPadding(new Insets(10));

        // Enrollments are somewhat complex because we can view them per student, but here we can just show a list of all enrollments
        TableView<Enrollment> table = new TableView<>();
        TableColumn<Enrollment, String> colStudent = new TableColumn<>("Student");
        // Enrollment currently might just hold the Course and Grade, or it's mapped per student.
        // Let's check: enrollmentService.getEnrollmentsForStudent() returns List<Enrollment> which has getCourse(), getGrade().
        TableColumn<Enrollment, String> colCourse = new TableColumn<>("Course Code");
        colCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getCourseCode().getCode()));
        TableColumn<Enrollment, String> colTitle = new TableColumn<>("Course Title");
        colTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getTitle()));
        TableColumn<Enrollment, String> colGrade = new TableColumn<>("Grade");
        colGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade() != null ? c.getValue().getGrade().toString() : "Not Graded"));

        table.getColumns().addAll(colCourse, colTitle, colGrade);
        table.setPrefHeight(650);

        btnEnroll.setOnAction(e -> {
            try {
                enrollmentService.enrollStudent(regNoField.getText(), new CourseCode(courseCodeField.getText()));
                showAlert(Alert.AlertType.INFORMATION, "Enrolled successfully.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Enrollment failed: " + ex.getMessage());
            }
        });

        btnUnenroll.setOnAction(e -> {
            try {
                enrollmentService.unenrollStudent(regNoField.getText(), new CourseCode(courseCodeField.getText()));
                showAlert(Alert.AlertType.INFORMATION, "Unenrolled successfully.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Unenrollment failed: " + ex.getMessage());
            }
        });

        btnRecordGrade.setOnAction(e -> {
            try {
                if (gradeCombo.getValue() == null) throw new IllegalArgumentException("Select a grade first.");
                enrollmentService.recordGrade(regNoField.getText(), new CourseCode(courseCodeField.getText()), gradeCombo.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Grade recorded successfully.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to record grade: " + ex.getMessage());
            }
        });

        TextField searchRegNo = new TextField();
        searchRegNo.setPromptText("Enter Reg No...");
        Button btnView = new Button("View Enrollments", new FontIcon("fas-search"));
        btnView.setOnAction(e -> {
            if(searchRegNo.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Enter a Reg No first.");
                return;
            }
            runTaskWithProgress("Fetching Enrollments...", () -> {
                // Simulate heavy operation
                Thread.sleep(2000); 
                List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(searchRegNo.getText());
                Platform.runLater(() -> table.getItems().setAll(enrollments));
            }, null);
        });
        
        HBox topBox = new HBox(10, new Label("Search:"), searchRegNo, btnView);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        VBox rightPane = new VBox(topBox, table);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(rightPane, leftPane);
        splitPane.setDividerPositions(0.70);

        layout.setCenter(splitPane);
        
        List<String> studentDict = new ArrayList<>();
        List<String> courseDict = new ArrayList<>();

                runTaskWithProgress("Loading Dictionaries...", () -> {
                    List<String> sList = studentService.getAllStudentsSortedById().stream()
                        .map(s -> s.getRegNo() + " - " + s.getFullName())
                        .collect(Collectors.toList());
                    List<String> cList = courseService.getAllCoursesSortedByCode().stream()
                        .map(c -> c.getCourseCode().getCode() + " - " + c.getTitle())
                        .collect(Collectors.toList());
                    Platform.runLater(() -> {
                        studentDict.clear();
                        studentDict.addAll(sList);
                        courseDict.clear();
                        courseDict.addAll(cList);
                    });
                }, null);


        setupAutocomplete(regNoField, () -> studentDict);
        setupAutocomplete(searchRegNo, () -> studentDict);
        setupAutocomplete(courseCodeField, () -> courseDict);
        
        
        return layout;
    }

    // =========================================================================
    // FILE OPERATIONS TAB
    // =========================================================================
    private javafx.scene.Node createFileOperationsView() {
        Tab tab = new Tab("File & System Ops");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        String[] instReq = {"FiD", "firstName", "lastName", "email", "department", "dob", "phone", "cabinNo"};
        String[] coursesReq = {"code", "title", "credits", "department", "instructorId", "semester", "classroomNo"};
        String[] studentsReq = {"id", "regNo", "firstName", "lastName", "email", "status", "registrationDate", "dob", "phone"};
        String[] enrollReq = {"studentRegNo", "courseCode", "grade"};

        Button btnImportCourses = new Button("Import Courses");
        btnImportCourses.setOnAction(e -> handleSingleImport("Courses", coursesReq, importExportService::importCoursesFile));

        Button btnImportStudents = new Button("Import Students");
        btnImportStudents.setOnAction(e -> handleSingleImport("Students", studentsReq, importExportService::importStudentsFile));

        Button btnImportInstructors = new Button("Import Instructors");
        btnImportInstructors.setOnAction(e -> handleSingleImport("Instructors", instReq, importExportService::importInstructorsFile));

        Button btnImportEnrollments = new Button("Import Enrollments");
        btnImportEnrollments.setOnAction(e -> handleSingleImport("Enrollments", enrollReq, importExportService::importEnrollmentsFile));

        Button btnImportAll = new Button("Import All");
        btnImportAll.setOnAction(e -> handleImportAll());

        Button btnExport = new Button("Export Data");
        btnExport.setOnAction(e -> runTaskWithProgress("Exporting Data...", 
            importExportService::exportData, () -> showAlert(Alert.AlertType.INFORMATION, "Data exported.")));

        Button btnBackup = new Button("Create Backup");
        btnBackup.setOnAction(e -> runTaskWithProgress("Creating Backup...", 
            backupService::performBackup, () -> showAlert(Alert.AlertType.INFORMATION, "Backup created successfully.")));

        Button btnShowBackupSize = new Button("Show Backup Directory Size");
        btnShowBackupSize.setOnAction(e -> {
            Path backupDir = AppConfig.getInstance().getBackupDirectory();
            try {
                if (Files.exists(backupDir)) {
                    long size = RecursiveUtil.calculateDirectorySize(backupDir);
                    showAlert(Alert.AlertType.INFORMATION, String.format("Total size of backups directory '%s' is %,d bytes.", backupDir, size));
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Backup directory does not exist yet.");
                }
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Could not calculate size: " + ex.getMessage());
            }
        });
        
        Button btnDeleteDb = new Button("Clear All Data", new FontIcon("fas-trash"));
        btnDeleteDb.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDeleteDb.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear All Data");
            alert.setHeaderText("ARE YOU SURE?");
            alert.setContentText("This will permanently DELETE all records (students, instructors, courses, enrollments).\nThe database tables will remain intact — you can import new data immediately after.");
            alert.showAndWait().ifPresent(response -> {
                if(response == ButtonType.OK) {
                    runTaskWithProgress("Clearing all data...", () -> dbAdminService.clearAllData(),
                        () -> showAlert(Alert.AlertType.INFORMATION, "All data cleared successfully. The schema is intact — you can import new data now."));
                }
            });
        });

        HBox row1 = new HBox(10, btnImportCourses, btnImportStudents, btnImportInstructors, btnImportEnrollments);
        HBox row2 = new HBox(10, btnImportAll, btnExport, btnBackup, btnShowBackupSize);
        HBox row3 = new HBox(10, btnDeleteDb);

        layout.getChildren().addAll(
            new Label("Import / Export Operations"), row1, row2,
            new Separator(),
            new Label("System Actions"), row3
        );

        
        return layout;
    }
    
    private void setupAutocomplete(TextField textField, java.util.function.Supplier<List<String>> dictionarySupplier) {
        ContextMenu contextMenu = new ContextMenu();
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                contextMenu.hide();
            } else {
                contextMenu.getItems().clear();
                String filter = newValue.toLowerCase();
                List<String> dict = dictionarySupplier.get();
                for (String item : dict) {
                    if (item.toLowerCase().contains(filter)) {
                        MenuItem menuItem = new MenuItem(item);
                        menuItem.setOnAction(e -> {
                            String[] parts = item.split(" - ");
                            textField.setText(parts[0]);
                            textField.positionCaret(parts[0].length());
                        });
                        contextMenu.getItems().add(menuItem);
                        if (contextMenu.getItems().size() >= 10) break;
                    }
                }
                if (contextMenu.getItems().isEmpty()) {
                    contextMenu.hide();
                } else {
                    if (!contextMenu.isShowing() && textField.getScene() != null && textField.getScene().getWindow() != null) {
                        contextMenu.show(textField, javafx.geometry.Side.BOTTOM, 0, 0);
                    }
                }
            }
        });

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                contextMenu.hide();
            }
        });
    }
}
