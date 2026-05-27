package edu.ccrm.gui;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.*;
import edu.ccrm.io.BackupService;
import edu.ccrm.io.DatabaseInitializer;
import edu.ccrm.io.ImportExportService;
import edu.ccrm.service.*;
import edu.ccrm.util.RecursiveUtil;
import java.time.LocalDate;

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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private final StudentService studentService = new StudentService();
    private final InstructorService instructorService = new InstructorService();
    private final CourseService courseService = new CourseService(instructorService);
    private final EnrollmentService enrollmentService = new EnrollmentService(studentService, courseService);
    private final TranscriptService transcriptService = new TranscriptService(studentService, enrollmentService);
    private final ProbationService probationService = new ProbationService();
    private final ImportExportService importExportService = new ImportExportService(studentService, instructorService, courseService, enrollmentService, probationService);
    private final BackupService backupService = new BackupService();
    private final DatabaseAdminService dbAdminService = new DatabaseAdminService();
    private final AnalyticsService analyticsService = new AnalyticsService();

    private final TextField probRegsField = new TextField();
    private final TextArea probReasonArea = new TextArea();
    private Runnable refreshStudentsTable;
    private Runnable refreshProbTable;
    private ProbationReport editingProbationReport = null;

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

        progressBox = new VBox(10, progressHeading, centerSpinner, progressTextLabel, centerProgressBar, recordProgressLabel, recordProgressBar);
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
        Button btnCohortGrading = createNavButton("Cohort Grading", "fas-calculator");
        Button btnInsights = createNavButton("Performance Insights", "fas-chart-bar");
        Button btnSystem = createNavButton("System & Backup Ops", "fas-database");

        sidebar.getChildren().addAll(new Separator(), btnDashboard, btnStudents, btnInstructors, btnCourses, btnEnrollments, btnCohortGrading, btnInsights, btnSystem);
        mainLayout.setLeft(sidebar);

        btnDashboard.setOnAction(e -> mainLayout.setCenter(createDashboardView()));
        btnStudents.setOnAction(e -> mainLayout.setCenter(createStudentsOptionsView()));
        btnInstructors.setOnAction(e -> mainLayout.setCenter(createInstructorsView()));
        btnCourses.setOnAction(e -> mainLayout.setCenter(createCoursesView()));
        btnEnrollments.setOnAction(e -> mainLayout.setCenter(createEnrollmentsView()));
        btnCohortGrading.setOnAction(e -> mainLayout.setCenter(createCohortGradingView()));
        btnInsights.setOnAction(e -> mainLayout.setCenter(createPerformanceInsightsView()));
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
                Dialog<java.util.Map<String, Boolean>> sampleDataDialog = new Dialog<>();
                sampleDataDialog.setTitle("Setup Database");
                sampleDataDialog.setHeaderText("Welcome to CCRM!\nChoose which tables to load sample data into. Unselected tables will remain empty.");

                ButtonType importButtonType = new ButtonType("Load Selected", ButtonBar.ButtonData.OK_DONE);
                sampleDataDialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                CheckBox cbInstructors = new CheckBox("Instructors");
                cbInstructors.setSelected(true);
                CheckBox cbCourses = new CheckBox("Courses");
                cbCourses.setSelected(true);
                CheckBox cbStudents = new CheckBox("Students");
                cbStudents.setSelected(true);
                CheckBox cbEnrollments = new CheckBox("Enrollments");
                cbEnrollments.setSelected(true);
                CheckBox cbProbation = new CheckBox("Probation Reports");
                cbProbation.setSelected(true);

                grid.add(new Label("Select tables to initialize with sample data:"), 0, 0, 2, 1);
                grid.add(cbInstructors, 0, 1);
                grid.add(cbCourses, 0, 2);
                grid.add(cbStudents, 0, 3);
                grid.add(cbEnrollments, 0, 4);
                grid.add(cbProbation, 0, 5);

                sampleDataDialog.getDialogPane().setContent(grid);

                sampleDataDialog.setResultConverter(dialogButton -> {
                    if (dialogButton == importButtonType) {
                        java.util.Map<String, Boolean> selections = new java.util.HashMap<>();
                        selections.put("instructors", cbInstructors.isSelected());
                        selections.put("courses", cbCourses.isSelected());
                        selections.put("students", cbStudents.isSelected());
                        selections.put("enrollments", cbEnrollments.isSelected());
                        selections.put("probation", cbProbation.isSelected());
                        return selections;
                    }
                    return null;
                });

                sampleDataDialog.showAndWait().ifPresent(selections -> {
                    runTaskWithProgress("Loading sample data...", () -> {
                        DatabaseInitializer.importSampleData(
                            selections.get("instructors"),
                            selections.get("courses"),
                            selections.get("students"),
                            selections.get("enrollments"),
                            selections.get("probation")
                        );
                    }, () -> showAlert(Alert.AlertType.INFORMATION, "Sample data setup complete for selected tables!"));
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
            createMetricCard("On Probation", String.valueOf(prob), "fas-exclamation-triangle"),
            createMetricCard("Graduated", String.valueOf(graduated), "fas-graduation-cap")
        );
        
        HBox contentRow = new HBox(25);
        contentRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        // Chart (taking left half)
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.BarChart<String,Number> bc = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        bc.setTitle("Students by Status");
        bc.setLegendVisible(false);
        bc.setMinWidth(400);
        HBox.setHgrow(bc, Priority.ALWAYS);

        javafx.scene.chart.XYChart.Series<String,Number> series1 = new javafx.scene.chart.XYChart.Series<>();
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("ACTIVE", active));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("PROBATION", prob));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("INACTIVE", susp));
        series1.getData().add(new javafx.scene.chart.XYChart.Data<>("GRADUATED", graduated));
        bc.getData().add(series1);

        // Toppers list (taking right half)
        VBox toppersCard = new VBox(15);
        toppersCard.setPadding(new Insets(20));
        toppersCard.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        toppersCard.setPrefWidth(500);
        toppersCard.setMinWidth(400);
        HBox.setHgrow(toppersCard, Priority.ALWAYS);

        Label toppersTitle = new Label("Academic Toppers (Top 15 Active)", new FontIcon("fas-trophy"));
        toppersTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        
        VBox toppersListContainer = new VBox(10);
        toppersListContainer.setAlignment(Pos.TOP_LEFT);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(toppersListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        java.util.List<Student> toppers = allStudents.stream()
                .filter(s -> s.getStatus() == Student.Status.ACTIVE)
                .filter(s -> s.getCgpa() != null && s.getCgpa() > 0.0)
                .sorted((s1, s2) -> Double.compare(s2.getCgpa(), s1.getCgpa()))
                .limit(15)
                .collect(Collectors.toList());

        if (toppers.isEmpty()) {
            Label placeholder = new Label("No CGPA data computed yet.\nClick 'Calculate CGPAs' below to process records.");
            placeholder.setStyle("-fx-text-fill: -color-fg-muted; -fx-alignment: center; -fx-text-alignment: center; -fx-font-style: italic;");
            placeholder.setPadding(new Insets(30, 0, 30, 0));
            placeholder.setMaxWidth(Double.MAX_VALUE);
            toppersListContainer.getChildren().add(placeholder);
        } else {
            int rank = 1;
            for (Student s : toppers) {
                HBox row = new HBox(15);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 6px; -fx-border-color: -color-border-muted; -fx-border-width: 1px;");
                
                Label rankLbl = new Label("#" + rank);
                rankLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-warning-fg; -fx-font-size: 14px;");
                
                VBox nameBox = new VBox(2);
                Label nameLbl = new Label(s.getFullName().toString());
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label regLbl = new Label(s.getRegNo() + " | " + s.getStatus());
                regLbl.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");
                nameBox.getChildren().addAll(nameLbl, regLbl);
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                Label gpaLbl = new Label(String.format("%.2f", s.getCgpa()));
                gpaLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -color-success-fg;");
                
                row.getChildren().addAll(rankLbl, nameBox, spacer, gpaLbl);
                toppersListContainer.getChildren().add(row);
                rank++;
            }
        }

        Button btnCalc = new Button("Calculate & Save CGPAs", new FontIcon("fas-calculator"));
        btnCalc.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");
        btnCalc.setMaxWidth(Double.MAX_VALUE);
        
        btnCalc.setOnAction(e -> {
            Task<Void> calcTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    java.util.List<Student> students = studentService.getAllStudentsSortedById();
                    int total = students.size();
                    for (int i = 0; i < total; i++) {
                        Student s = students.get(i);
                        updateMessage("Processing student " + s.getFullName() + " (" + (i + 1) + "/" + total + ")...");
                        updateProgress(i + 1, total);
                        
                        java.util.List<edu.ccrm.domain.Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(s.getRegNo());
                        double cgpa = transcriptService.calculateCGPA(enrollments);
                        
                        studentService.updateStudentCgpa(s.getRegNo(), cgpa);
                    }
                    return null;
                }
            };
            
            runJavaFXTask(calcTask, "Calculating Student CGPAs...", () -> {
                showAlert(Alert.AlertType.INFORMATION, "CGPAs computed and saved successfully for all students!");
                mainLayout.setCenter(createDashboardView());
            });
        });

        toppersCard.getChildren().addAll(toppersTitle, scrollPane, btnCalc);
        contentRow.getChildren().addAll(bc, toppersCard);

        layout.getChildren().addAll(title, cardsBox, contentRow);
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
            if (headers.length > 0 && headers[0].startsWith("\ufeff")) {
                headers[0] = headers[0].substring(1);
            }
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

    private List<String[]> parseCsv(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> values = new ArrayList<>();
                boolean inQuotes = false;
                StringBuilder curVal = new StringBuilder();
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c == '\"') {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        values.add(curVal.toString().trim());
                        curVal.setLength(0);
                    } else {
                        curVal.append(c);
                    }
                }
                values.add(curVal.toString().trim());
                rows.add(values.toArray(new String[0]));
            }
        }
        if (!rows.isEmpty()) {
            String[] firstRow = rows.get(0);
            if (firstRow.length > 0 && firstRow[0].startsWith("\ufeff")) {
                firstRow[0] = firstRow[0].substring(1);
            }
        }
        return rows;
    }

    private boolean showCsvPreview(Path path, String moduleName) {
        try {
            List<String[]> rows = parseCsv(path);
            if (rows.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "The selected CSV file is empty.");
                return false;
            }
            String[] headers = rows.get(0);
            int totalRecords = Math.max(0, rows.size() - 1);
            
            Stage dialog = new Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(mainLayout.getScene().getWindow());
            dialog.setTitle("CSV File Preview - " + path.getFileName());
            
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: -color-bg-default;");
            
            Label titleLabel = new Label("CSV Data Preview: " + moduleName);
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
            
            Label infoLabel = new Label(String.format("File: %s\nTotal Records Detected: %d\nBelow is a preview of the first 10 rows.", path.getFileName(), totalRecords));
            infoLabel.setStyle("-fx-font-size: 13px;");
            
            TableView<String[]> previewTable = new TableView<>();
            previewTable.setMinHeight(250);
            previewTable.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(previewTable, Priority.ALWAYS);
            previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            for (int i = 0; i < headers.length; i++) {
                final int colIndex = i;
                TableColumn<String[], String> column = new TableColumn<>(headers[i]);
                column.setCellValueFactory(cd -> {
                    String[] rowData = cd.getValue();
                    String val = (rowData != null && colIndex < rowData.length) ? rowData[colIndex] : "";
                    return new javafx.beans.property.SimpleStringProperty(val);
                });
                previewTable.getColumns().add(column);
            }
            
            ObservableList<String[]> previewData = FXCollections.observableArrayList();
            for (int i = 1; i <= Math.min(10, totalRecords); i++) {
                previewData.add(rows.get(i));
            }
            previewTable.setItems(previewData);
            
            Button btnProceed = new Button("Proceed with Import", new FontIcon("fas-check-circle"));
            btnProceed.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");
            
            Button btnCancel = new Button("Cancel / Reupload", new FontIcon("fas-times-circle"));
            btnCancel.getStyleClass().add("flat");
            
            final boolean[] approved = new boolean[1];
            
            btnProceed.setOnAction(e -> {
                approved[0] = true;
                dialog.close();
            });
            
            btnCancel.setOnAction(e -> {
                approved[0] = false;
                dialog.close();
            });
            
            HBox buttons = new HBox(15, btnProceed, btnCancel);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            
            root.getChildren().addAll(titleLabel, infoLabel, previewTable, buttons);
            
            Scene scene = new Scene(root, 850, 500);
            dialog.setScene(scene);
            dialog.showAndWait();
            
            return approved[0];
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Failed to parse CSV file for preview: " + ex.getMessage());
            return false;
        }
    }

    private File promptAndPreviewFile(FileChooser chooser, String stepName, String[] requiredFields) {
        while (true) {
            File file = chooser.showOpenDialog(mainLayout.getScene().getWindow());
            if (file == null) {
                return null;
            }
            if (!verifyHeader(file.toPath(), requiredFields)) {
                showAlert(Alert.AlertType.ERROR, "Invalid CSV format for " + stepName + ".\nExpected header fields:\n" + String.join(", ", requiredFields));
                continue;
            }
            if (!showCsvPreview(file.toPath(), stepName)) {
                continue;
            }
            return file;
        }
    }

    private void handleSingleImport(String moduleName, String[] requiredFields, FileImportAction importAction) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select " + moduleName + " CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        while (true) {
            File file = chooser.showOpenDialog(mainLayout.getScene().getWindow());
            if (file == null) {
                return;
            }
            if (!verifyHeader(file.toPath(), requiredFields)) {
                showAlert(Alert.AlertType.ERROR, "Invalid CSV format for " + moduleName + ".\nExpected header fields (in any order):\n" + String.join(", ", requiredFields));
                continue;
            }
            if (!showCsvPreview(file.toPath(), moduleName)) {
                continue;
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
            break;
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
        prompt.setContentText("You are about to bulk import all data. You will be prompted to select and preview 5 files in the exact order needed by the database:\n\n1. Instructors\n2. Courses\n3. Students\n4. Enrollments\n5. Probation Reports\n\nClick OK to select the first file (Instructors).");
        prompt.showAndWait();

        String[] instReq = {"FiD", "firstName", "lastName", "email", "department", "dob", "phone", "cabinNo"};
        String[] coursesReq = {"code", "title", "credits", "department", "instructorId", "semester", "classroomNo"};
        String[] studentsReq = {"id", "regNo", "firstName", "lastName", "email", "status", "registrationDate", "dob", "phone"};
        String[] enrollReq = {"studentRegNo", "courseCode", "grade"};
        String[] probationReq = {"probationId", "studentRegNos", "startDate", "endDate", "reason"};

        chooser.setTitle("1. Select Instructors CSV File");
        File instFile = promptAndPreviewFile(chooser, "Instructors", instReq);
        if (instFile == null) return;
        
        prompt.setHeaderText("Next: Courses");
        prompt.setContentText("Instructors selected successfully.\n\nClick OK to select the second file (Courses).");
        prompt.showAndWait();

        chooser.setTitle("2. Select Courses CSV File");
        File coursesFile = promptAndPreviewFile(chooser, "Courses", coursesReq);
        if (coursesFile == null) return;
        
        prompt.setHeaderText("Next: Students");
        prompt.setContentText("Courses selected successfully.\n\nClick OK to select the third file (Students).");
        prompt.showAndWait();

        chooser.setTitle("3. Select Students CSV File");
        File studentsFile = promptAndPreviewFile(chooser, "Students", studentsReq);
        if (studentsFile == null) return;
        
        prompt.setHeaderText("Next: Enrollments");
        prompt.setContentText("Students selected successfully.\n\nClick OK to select the fourth file (Enrollments).");
        prompt.showAndWait();

        chooser.setTitle("4. Select Enrollments CSV File");
        File enrollFile = promptAndPreviewFile(chooser, "Enrollments", enrollReq);
        if (enrollFile == null) return;

        prompt.setHeaderText("Next: Probation Reports");
        prompt.setContentText("Enrollments selected successfully.\n\nClick OK to select the final file (Probation Reports).");
        prompt.showAndWait();

        chooser.setTitle("5. Select Probation Reports CSV File");
        File probationFile = promptAndPreviewFile(chooser, "Probation Reports", probationReq);
        if (probationFile == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 5);
                
                Platform.runLater(() -> {
                    recordProgressBar.setVisible(true);
                    recordProgressLabel.setVisible(true);
                });

                updateMessage("Importing Instructors (1/5)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importInstructorsFile(instFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(1, 5);
                Thread.sleep(300);

                updateMessage("Importing Courses (2/5)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importCoursesFile(coursesFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(2, 5);
                Thread.sleep(300);

                updateMessage("Importing Students (3/5)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importStudentsFile(studentsFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(3, 5);
                Thread.sleep(300);

                updateMessage("Importing Enrollments (4/5)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importEnrollmentsFile(enrollFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(4, 5);
                Thread.sleep(300);

                updateMessage("Importing Probation Reports (5/5)...");
                Platform.runLater(() -> { recordProgressBar.setProgress(0); recordProgressLabel.setText("Starting..."); });
                importExportService.importProbationReportsFile(probationFile.toPath(), (p, t) -> {
                    double frac = t > 0 ? (double) p / t : 0;
                    int pct = t > 0 ? (int)((p * 100L) / t) : 0;
                    Platform.runLater(() -> {
                        recordProgressBar.setProgress(frac);
                        recordProgressLabel.setText(String.format("Record %d / %d (%d%%)", p, t, pct));
                    });
                });
                updateProgress(5, 5);
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
    private javafx.scene.Node createStudentsOptionsView() {
        VBox layout = new VBox(30);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Student Management Options");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");

        HBox optionsBox = new HBox(40);
        optionsBox.setAlignment(Pos.CENTER);

        // Card 1: Students Directory
        VBox cardDirectory = createOptionCard(
            "Students Directory",
            "Add and manage student registration profiles,\ndetails, status updates, and transcripts.",
            "fas-users",
            e -> mainLayout.setCenter(createStudentsDirectoryView())
        );

        // Card 2: Probation Reports
        VBox cardProbation = createOptionCard(
            "Probation Reports",
            "Put students on probationary periods,\nlog reasons, and view historical reports.",
            "fas-exclamation-triangle",
            e -> mainLayout.setCenter(createProbationReportsView())
        );

        optionsBox.getChildren().addAll(cardDirectory, cardProbation);

        layout.getChildren().addAll(title, optionsBox);
        return layout;
    }

    private VBox createOptionCard(String title, String description, String iconCode, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(350, 250);
        card.setStyle(
            "-fx-background-color: -color-bg-default; " +
            "-fx-border-color: -color-border-default; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px; " +
            "-fx-background-radius: 12px; " +
            "-fx-cursor: hand;"
        );

        // Hover animations
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: -color-bg-subtle; " +
                "-fx-border-color: -color-accent-emphasis; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-cursor: hand;"
            );
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: -color-bg-default; " +
                "-fx-border-color: -color-border-default; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-cursor: hand;"
            );
            card.setTranslateY(0);
        });

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(48);
        icon.setStyle("-fx-icon-color: -color-accent-fg;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

        Label descLbl = new Label(description);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted; -fx-text-alignment: center;");
        descLbl.setWrapText(true);

        Button btnAction = new Button("Open Options", new FontIcon("fas-arrow-right"));
        btnAction.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");
        btnAction.setOnAction(onAction);

        // Clicking anywhere on the card triggers the action
        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                btnAction.fire();
            }
        });

        card.getChildren().addAll(icon, titleLbl, descLbl, btnAction);
        return card;
    }

    private javafx.scene.Node createStudentsDirectoryView() {
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 20, 10));
        
        Button btnBack = new Button("Back to Student Options", new FontIcon("fas-arrow-left"));
        btnBack.getStyleClass().add("flat");
        btnBack.setOnAction(e -> mainLayout.setCenter(createStudentsOptionsView()));
        
        Label titleLabel = new Label("Students Directory");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        header.getChildren().addAll(btnBack, titleLabel, headerSpacer);
        mainPane.setTop(header);

        // Core Student Directory View Content
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(0));

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
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

        TableColumn<Student, String> colCgpa = new TableColumn<>("CGPA");
        colCgpa.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCgpa() != null ? String.format("%.2f", c.getValue().getCgpa()) : "N/A"));
        colCgpa.setCellFactory(column -> new TableCell<Student, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(item);
                    if (!"N/A".equals(item)) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: -color-success-fg;");
                    } else {
                        setStyle("-fx-text-fill: -color-fg-muted;");
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Student, Integer> colProbCount = new TableColumn<>("Probations");
        colProbCount.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getProbationCount()));
        colProbCount.setCellFactory(column -> new TableCell<Student, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    if (item > 0) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: -color-warning-fg;");
                    } else {
                        setStyle("-fx-text-fill: -color-fg-muted;");
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Student, Integer> colGradedCredits = new TableColumn<>("Credits Completed");
        colGradedCredits.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getGradedCredits()));
        colGradedCredits.setCellFactory(column -> new TableCell<Student, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-font-weight: bold;");
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colId, colRegNo, colName, colEmail, colDob, colPhone, colRegDate, colStatus, colCgpa, colGradedCredits, colProbCount);
        table.setMinHeight(300);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

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

        refreshStudentsTable = () -> {
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
                refreshStudentsTable.run();
                idField.clear(); regNoField.clear(); firstNameField.clear(); lastNameField.clear(); emailField.clear(); phoneField.clear(); dobPicker.setValue(null); regDatePicker.setValue(null);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to add student: " + ex.getMessage());
            }
        });

        // Bottom Actions
        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.setOnAction(e -> refreshStudentsTable.run());

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
                        coursesTable.setMaxHeight(Double.MAX_VALUE);
                        VBox.setVgrow(coursesTable, Priority.ALWAYS);

                        Label cgpaLabel = new Label(String.format("CGPA: %.2f", cgpa));
                        cgpaLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        cgpaLabel.setStyle("-fx-text-fill: white;");

                        content.getChildren().addAll(headerInfo, coursesTable, cgpaLabel);
                        VBox.setVgrow(coursesTable, Priority.ALWAYS);

                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.setResizable(true);
                        dialog.getDialogPane().setPrefSize(1100, 750);
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
                    refreshStudentsTable.run();
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

            MenuItem calcCgpa = new MenuItem("Calculate CGPA");
            calcCgpa.setGraphic(new FontIcon("fas-calculator"));
            calcCgpa.setOnAction(e -> {
                Student selected = row.getItem();
                if (selected != null) {
                    Task<Void> calcSingleTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            updateMessage("Fetching enrollments...");
                            java.util.List<edu.ccrm.domain.Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(selected.getRegNo());
                            updateMessage("Computing CGPA...");
                            double cgpa = transcriptService.calculateCGPA(enrollments);
                            updateMessage("Saving to database...");
                            studentService.updateStudentCgpa(selected.getRegNo(), cgpa);
                            return null;
                        }
                    };
                    
                    runJavaFXTask(calcSingleTask, "Calculating CGPA for " + selected.getFullName() + "...", () -> {
                        showAlert(Alert.AlertType.INFORMATION, "CGPA calculated and saved successfully for " + selected.getFullName() + "!");
                        refreshStudentsTable.run();
                    });
                }
            });
            
            MenuItem addProbationReport = new MenuItem("Add Probation Report");
            addProbationReport.setGraphic(new FontIcon("fas-exclamation-circle"));
            addProbationReport.setOnAction(e -> {
                ObservableList<Student> selected = table.getSelectionModel().getSelectedItems();
                if (selected != null && !selected.isEmpty()) {
                    String regNos = selected.stream()
                        .map(Student::getRegNo)
                        .collect(Collectors.joining(", "));
                    probRegsField.setText(regNos);
                    mainLayout.setCenter(createProbationReportsView());
                    probReasonArea.requestFocus();
                }
            });
            
            contextMenu.getItems().addAll(viewTranscript, changeStatus, calcCgpa, addProbationReport);
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            if (!row.isEmpty()) {
                row.setContextMenu(contextMenu);
            }
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
        mainPane.setCenter(layout);

        // Initial table load
        refreshStudentsTable.run();

        return mainPane;
    }

    private javafx.scene.Node createProbationReportsView() {
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 20, 10));
        
        Button btnBack = new Button("Back to Student Options", new FontIcon("fas-arrow-left"));
        btnBack.getStyleClass().add("flat");
        btnBack.setOnAction(e -> mainLayout.setCenter(createStudentsOptionsView()));
        
        Label titleLabel = new Label("Probation Reports");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        header.getChildren().addAll(btnBack, titleLabel, headerSpacer);
        mainPane.setTop(header);

        // Core Probation View Content
        BorderPane probationLayout = new BorderPane();
        probationLayout.setPadding(new Insets(0));

        // Form (Right Pane)
        GridPane probForm = new GridPane();
        probForm.setHgap(10);
        probForm.setVgap(10);

        TextField probIdField = new TextField("PROB-" + (System.currentTimeMillis() % 10000));
        probRegsField.setPromptText("Enter comma-separated Reg Nos...");
        DatePicker probStartPicker = new DatePicker(LocalDate.now());
        DatePicker probEndPicker = new DatePicker(LocalDate.now().plusMonths(6));
        probReasonArea.setPromptText("Enter probation reason...");
        probReasonArea.setPrefRowCount(3);

        probForm.addRow(0, new Label("Probation ID:"), probIdField);
        probForm.addRow(1, new Label("Student Reg Nos:"), probRegsField);
        probForm.addRow(2, new Label("Start Date:"), probStartPicker);
        probForm.addRow(3, new Label("End Date:"), probEndPicker);
        probForm.addRow(4, new Label("Reason:"), probReasonArea);

        Button btnCreateProb = new Button("Create Probation Report", new FontIcon("fas-plus-circle"));
        btnCreateProb.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");
        
        Button btnCancel = new Button("Cancel", new FontIcon("fas-times"));
        HBox buttonBox = new HBox(10, btnCreateProb);
        probForm.add(buttonBox, 0, 5, 2, 1);

        TitledPane probFormPane = new TitledPane("New Probation Report", probForm);
        btnCancel.setOnAction(ev -> {
            editingProbationReport = null;
            probIdField.setDisable(false);
            probIdField.setText("PROB-" + (System.currentTimeMillis() % 10000));
            probRegsField.clear();
            probReasonArea.clear();
            probStartPicker.setValue(LocalDate.now());
            probEndPicker.setValue(LocalDate.now().plusMonths(6));
            probFormPane.setText("New Probation Report");
            btnCreateProb.setText("Create Probation Report");
            btnCreateProb.setGraphic(new FontIcon("fas-plus-circle"));
            buttonBox.getChildren().remove(btnCancel);
        });
        probFormPane.setCollapsible(false);
        VBox probLeftFormPane = new VBox(probFormPane);
        probLeftFormPane.setPadding(new Insets(10));

        // Table (Left Pane)
        TableView<ProbationReport> probTable = new TableView<>();
        TableColumn<ProbationReport, String> colProbId = new TableColumn<>("Probation ID");
        colProbId.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getProbationId()));

        TableColumn<ProbationReport, String> colProbRegs = new TableColumn<>("Student Reg Nos");
        colProbRegs.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.join(", ", c.getValue().getStudentRegNos())));

        TableColumn<ProbationReport, String> colProbStart = new TableColumn<>("Start Date");
        colProbStart.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStartDate().toString()));

        TableColumn<ProbationReport, String> colProbEnd = new TableColumn<>("End Date");
        colProbEnd.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEndDate().toString()));

        TableColumn<ProbationReport, String> colProbReason = new TableColumn<>("Reason");
        colProbReason.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getReason()));

        TableColumn<ProbationReport, String> colProbStatus = new TableColumn<>("Status");
        colProbStatus.setCellValueFactory(c -> {
            LocalDate today = LocalDate.now();
            ProbationReport r = c.getValue();
            if (today.isBefore(r.getStartDate())) {
                return new ReadOnlyStringWrapper("Pending");
            } else if (today.isAfter(r.getEndDate())) {
                return new ReadOnlyStringWrapper("Completed");
            } else {
                return new ReadOnlyStringWrapper("On Going");
            }
        });
        colProbStatus.setCellFactory(column -> new TableCell<ProbationReport, String>() {
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
                    if ("On Going".equals(item)) {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-warning-fg; -fx-background-color: -color-warning-muted;");
                    } else if ("Completed".equals(item)) {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-success-fg; -fx-background-color: -color-success-muted;");
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: -color-info-fg; -fx-background-color: -color-info-muted;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        probTable.getColumns().addAll(colProbId, colProbRegs, colProbStart, colProbEnd, colProbReason, colProbStatus);
        probTable.setMinHeight(300);
        probTable.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(probTable, Priority.ALWAYS);

        ObservableList<ProbationReport> probMasterData = FXCollections.observableArrayList();
        FilteredList<ProbationReport> probFilteredData = new FilteredList<>(probMasterData, p -> true);

        TextField probSearchField = new TextField();
        probSearchField.setPromptText("Search by ID, Reg No, Reason, or Status...");
        probSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            probFilteredData.setPredicate(report -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                
                LocalDate today = LocalDate.now();
                String status = today.isBefore(report.getStartDate()) ? "pending" : (today.isAfter(report.getEndDate()) ? "completed" : "on going");

                if (report.getProbationId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (report.getReason().toLowerCase().contains(lowerCaseFilter)) return true;
                if (status.contains(lowerCaseFilter)) return true;
                for (String reg : report.getStudentRegNos()) {
                    if (reg.toLowerCase().contains(lowerCaseFilter)) return true;
                }
                return false;
            });
        });

        SortedList<ProbationReport> probSortedData = new SortedList<>(probFilteredData);
        probSortedData.comparatorProperty().bind(probTable.comparatorProperty());
        probTable.setItems(probSortedData);

        probTable.setRowFactory(tv -> {
            TableRow<ProbationReport> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem updateReportMenu = new MenuItem("Update Probation Report");
            updateReportMenu.setGraphic(new FontIcon("fas-edit"));
            updateReportMenu.setOnAction(e -> {
                ProbationReport selected = row.getItem();
                if (selected != null) {
                    editingProbationReport = selected;
                    probIdField.setText(selected.getProbationId());
                    probIdField.setDisable(true);
                    probRegsField.setText(String.join(", ", selected.getStudentRegNos()));
                    probStartPicker.setValue(selected.getStartDate());
                    probEndPicker.setValue(selected.getEndDate());
                    probReasonArea.setText(selected.getReason());
                    probFormPane.setText("Update Probation Report");
                    btnCreateProb.setText("Update Probation Report");
                    btnCreateProb.setGraphic(new FontIcon("fas-save"));
                    if (!buttonBox.getChildren().contains(btnCancel)) {
                        buttonBox.getChildren().add(btnCancel);
                    }
                }
            });

            MenuItem deleteReportMenu = new MenuItem("Delete Probation Report");
            deleteReportMenu.setGraphic(new FontIcon("fas-trash-alt"));
            deleteReportMenu.setOnAction(e -> {
                ProbationReport selected = row.getItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Probation Report: " + selected.getProbationId());
                    confirm.setContentText("Are you sure you want to delete this probation report?\n" +
                                           "This will decrement the probation counts for the involved students and " +
                                           "restore their status to ACTIVE if they have no other active probations.");
                    confirm.showAndWait().ifPresent(btnType -> {
                        if (btnType == ButtonType.OK) {
                            Task<Void> deleteTask = new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    updateMessage("Deleting probation report...");
                                    probationService.deleteProbationReport(selected.getProbationId());
                                    return null;
                                }
                            };
                            runJavaFXTask(deleteTask, "Deleting Probation Report...", () -> {
                                showAlert(Alert.AlertType.INFORMATION, "Probation report deleted successfully!");
                                if (refreshProbTable != null) refreshProbTable.run();
                                if (refreshStudentsTable != null) refreshStudentsTable.run();
                            });
                        }
                    });
                }
            });
            contextMenu.getItems().addAll(updateReportMenu, deleteReportMenu);
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            if (!row.isEmpty()) {
                row.setContextMenu(contextMenu);
            }
            return row;
        });

        refreshProbTable = () -> {
            runTaskWithProgress("Loading Probation Reports...", () -> {
                List<ProbationReport> reports = probationService.getAllProbationReports();
                Platform.runLater(() -> probMasterData.setAll(reports));
            }, null);
        };

        // Submit Button Action
        btnCreateProb.setOnAction(e -> {
            try {
                String probId = probIdField.getText().trim();
                String regsStr = probRegsField.getText().trim();
                LocalDate start = probStartPicker.getValue();
                LocalDate end = probEndPicker.getValue();
                String reason = probReasonArea.getText().trim();

                if (probId.isEmpty() || regsStr.isEmpty() || reason.isEmpty() || start == null || end == null) {
                    throw new IllegalArgumentException("All fields are required.");
                }
                if (end.isBefore(start)) {
                    throw new IllegalArgumentException("End date cannot be before start date.");
                }

                List<String> regNos = java.util.Arrays.stream(regsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

                ProbationReport report = new ProbationReport(probId, start, end, reason, regNos);
                boolean isUpdate = (editingProbationReport != null);
                String taskTitle = isUpdate ? "Updating Probation Report..." : "Creating Probation Report...";
                String successMsg = isUpdate ? "Probation report updated successfully!" : "Probation report created successfully!";

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        int totalStudents = report.getStudentRegNos().size();
                        int totalSteps = totalStudents + 1;
                        int currentStep = 0;

                        // Verify all students exist and update progress
                        for (int i = 0; i < totalStudents; i++) {
                            String regNo = report.getStudentRegNos().get(i);
                            updateMessage(String.format("Verifying student %s (%d/%d)...", regNo, i + 1, totalStudents));
                            updateProgress(currentStep++, totalSteps);
                            studentService.findStudentByRegNo(regNo);
                            Thread.sleep(400); // Smooth animation delay
                        }

                        if (isUpdate) {
                            updateMessage("Updating report and modifying student statuses in database...");
                            updateProgress(currentStep++, totalSteps);
                            probationService.updateProbationReport(report);
                        } else {
                            updateMessage("Adding report and updating student statuses in database...");
                            updateProgress(currentStep++, totalSteps);
                            probationService.addProbationReport(report);
                        }
                        Thread.sleep(400); // Smooth transition delay

                        updateProgress(totalSteps, totalSteps);
                        updateMessage("Complete!");
                        Thread.sleep(200);
                        return null;
                    }
                };

                runJavaFXTask(task, taskTitle, () -> {
                    showAlert(Alert.AlertType.INFORMATION, successMsg);
                    
                    // Reset editing state and form
                    editingProbationReport = null;
                    probIdField.setDisable(false);
                    probIdField.setText("PROB-" + (System.currentTimeMillis() % 10000));
                    probRegsField.clear();
                    probReasonArea.clear();
                    probStartPicker.setValue(LocalDate.now());
                    probEndPicker.setValue(LocalDate.now().plusMonths(6));
                    probFormPane.setText("New Probation Report");
                    btnCreateProb.setText("Create Probation Report");
                    btnCreateProb.setGraphic(new FontIcon("fas-plus-circle"));
                    buttonBox.getChildren().remove(btnCancel);

                    if (refreshProbTable != null) refreshProbTable.run();
                    if (refreshStudentsTable != null) refreshStudentsTable.run();
                });
            } catch (Exception ex) {
                String action = (editingProbationReport != null) ? "update" : "create";
                showAlert(Alert.AlertType.ERROR, "Failed to " + action + " report: " + ex.getMessage());
            }
        });

        Button btnRefreshProb = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefreshProb.setOnAction(e -> refreshProbTable.run());

        HBox probTopBox = new HBox(10, new Label("Search:"), probSearchField, btnRefreshProb);
        probTopBox.setAlignment(Pos.CENTER_LEFT);
        probTopBox.setPadding(new Insets(0, 0, 10, 0));

        VBox probRightPane = new VBox(probTopBox, probTable);
        probRightPane.setPadding(new Insets(10));
        VBox.setVgrow(probTable, Priority.ALWAYS);

        SplitPane probSplitPane = new SplitPane();
        probSplitPane.getItems().addAll(probRightPane, probLeftFormPane);
        probSplitPane.setDividerPositions(0.70);
        probationLayout.setCenter(probSplitPane);

        mainPane.setCenter(probationLayout);

        // Initial table load
        refreshProbTable.run();

        return mainPane;
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
        table.setMinHeight(300);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

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
        table.setMinHeight(300);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

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
            if (!row.isEmpty()) {
                row.setContextMenu(contextMenu);
            }
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
        javafx.scene.control.Spinner<Integer> yearSpinner = new javafx.scene.control.Spinner<>(2000, 2100, java.time.LocalDate.now().getYear());
        yearSpinner.setEditable(true);
        ComboBox<Grade> gradeCombo = new ComboBox<>();
        gradeCombo.getItems().addAll(Grade.values());

        form.addRow(0, new Label("Student Reg No:"), regNoField);
        form.addRow(1, new Label("Course Code:"), courseCodeField);
        form.addRow(2, new Label("Year of Enrollment:"), yearSpinner);
        form.addRow(3, new Label("Grade (for recording):"), gradeCombo);

        Button btnEnroll = new Button("Enroll", new FontIcon("fas-user-check"));
        Button btnUnenroll = new Button("Unenroll", new FontIcon("fas-user-minus"));
        Button btnRecordGrade = new Button("Record Grade", new FontIcon("fas-pen"));

        javafx.scene.layout.FlowPane formActions = new javafx.scene.layout.FlowPane(10, 10);
        formActions.getChildren().addAll(btnEnroll, btnUnenroll, btnRecordGrade);
        form.add(formActions, 0, 4, 2, 1);

        TitledPane formPane = new TitledPane("Enrollment Actions", form);
        formPane.setCollapsible(false);
        VBox leftPane = new VBox(formPane);
        leftPane.setPadding(new Insets(10));

        // Enrollments are somewhat complex because we can view them per student, but here we can just show a list of all enrollments
        TableView<Enrollment> table = new TableView<>();
        TableColumn<Enrollment, String> colStudent = new TableColumn<>("Student");
        colStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(
            c.getValue().getStudent() != null
            ? c.getValue().getStudent().getRegNo() + " - " + c.getValue().getStudent().getFullName().toString()
            : "N/A"
        ));
        TableColumn<Enrollment, String> colCourse = new TableColumn<>("Course Code");
        colCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getCourseCode().getCode()));
        TableColumn<Enrollment, String> colTitle = new TableColumn<>("Course Title");
        colTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourse().getTitle()));
        TableColumn<Enrollment, String> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getEnrollmentYear())));
        TableColumn<Enrollment, String> colSemester = new TableColumn<>("Semester");
        colSemester.setCellValueFactory(c -> new ReadOnlyStringWrapper(
            c.getValue().getEnrollmentSemester() != null && !c.getValue().getEnrollmentSemester().isEmpty()
            ? c.getValue().getEnrollmentSemester() : "N/A"
        ));
        TableColumn<Enrollment, String> colGrade = new TableColumn<>("Grade");
        colGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade() != null ? c.getValue().getGrade().toString() : "Not Graded"));

        table.getColumns().addAll(colStudent, colCourse, colTitle, colYear, colSemester, colGrade);
        table.setMinHeight(300);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TextField searchRegNo = new TextField();
        searchRegNo.setPromptText("Enter Reg No or Course Code...");

        Runnable refreshEnrollments = () -> {
            String query = searchRegNo.getText().trim();
            if (query.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Enter a Reg No or Course Code first.");
                return;
            }
            runTaskWithProgress("Fetching Enrollments...", () -> {
                List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(query);
                if (enrollments.isEmpty()) {
                    enrollments = enrollmentService.getEnrollmentsForCourse(query);
                }
                final List<Enrollment> finalEnrollments = enrollments;
                Platform.runLater(() -> table.getItems().setAll(finalEnrollments));
            }, null);
        };

        btnEnroll.setOnAction(e -> {
            try {
                int enrollmentYear = yearSpinner.getValue();
                enrollmentService.enrollStudent(regNoField.getText(), new CourseCode(courseCodeField.getText()), enrollmentYear);
                showAlert(Alert.AlertType.INFORMATION, "Enrolled successfully.");
                String query = searchRegNo.getText().trim();
                if (regNoField.getText().trim().equalsIgnoreCase(query) || 
                    courseCodeField.getText().trim().equalsIgnoreCase(query)) {
                    refreshEnrollments.run();
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Enrollment failed: " + ex.getMessage());
            }
        });

        btnUnenroll.setOnAction(e -> {
            try {
                enrollmentService.unenrollStudent(regNoField.getText(), new CourseCode(courseCodeField.getText()));
                showAlert(Alert.AlertType.INFORMATION, "Unenrolled successfully.");
                String query = searchRegNo.getText().trim();
                if (regNoField.getText().trim().equalsIgnoreCase(query) || 
                    courseCodeField.getText().trim().equalsIgnoreCase(query)) {
                    refreshEnrollments.run();
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Unenrollment failed: " + ex.getMessage());
            }
        });

        btnRecordGrade.setOnAction(e -> {
            try {
                if (gradeCombo.getValue() == null) throw new IllegalArgumentException("Select a grade first.");
                enrollmentService.recordGrade(regNoField.getText(), new CourseCode(courseCodeField.getText()), gradeCombo.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Grade recorded successfully.");
                String query = searchRegNo.getText().trim();
                if (regNoField.getText().trim().equalsIgnoreCase(query) || 
                    courseCodeField.getText().trim().equalsIgnoreCase(query)) {
                    refreshEnrollments.run();
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to record grade: " + ex.getMessage());
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Enrollment> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem updateGradeItem = new MenuItem("Update Grade");
            updateGradeItem.setGraphic(new FontIcon("fas-pen"));
            updateGradeItem.setOnAction(evt -> {
                Enrollment selected = row.getItem();
                if (selected != null) {
                    ChoiceDialog<Grade> dialog = new ChoiceDialog<>(selected.getGrade(), Grade.values());
                    dialog.setTitle("Update Grade");
                    dialog.setHeaderText("Select new grade for student " + selected.getStudent().getRegNo() + " in " + selected.getCourse().getCourseCode().getCode());
                    dialog.showAndWait().ifPresent(newGrade -> {
                        try {
                            enrollmentService.recordGrade(selected.getStudent().getRegNo(), selected.getCourse().getCourseCode(), newGrade);
                            showAlert(Alert.AlertType.INFORMATION, "Grade updated successfully.");
                            refreshEnrollments.run();
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Failed to update grade: " + ex.getMessage());
                        }
                    });
                }
            });

            MenuItem updateYearItem = new MenuItem("Update Year of Enrollment");
            updateYearItem.setGraphic(new FontIcon("fas-calendar-alt"));
            updateYearItem.setOnAction(evt -> {
                Enrollment selected = row.getItem();
                if (selected != null) {
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getEnrollmentYear()));
                    dialog.setTitle("Update Year of Enrollment");
                    dialog.setHeaderText("Enter new enrollment year for student " + selected.getStudent().getRegNo() + " in " + selected.getCourse().getCourseCode().getCode());
                    dialog.showAndWait().ifPresent(yearStr -> {
                        try {
                            int newYear = Integer.parseInt(yearStr.trim());
                            enrollmentService.updateEnrollmentYear(selected.getStudent().getRegNo(), selected.getCourse().getCourseCode(), newYear);
                            showAlert(Alert.AlertType.INFORMATION, "Enrollment year updated successfully.");
                            refreshEnrollments.run();
                        } catch (NumberFormatException nfe) {
                            showAlert(Alert.AlertType.ERROR, "Invalid year format.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Failed to update enrollment year: " + ex.getMessage());
                        }
                    });
                }
            });

            contextMenu.getItems().addAll(updateGradeItem, updateYearItem);
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            if (!row.isEmpty()) {
                row.setContextMenu(contextMenu);
            }
            return row;
        });

        Button btnView = new Button("View Enrollments", new FontIcon("fas-search"));
        btnView.setOnAction(e -> refreshEnrollments.run());
        
        HBox topBox = new HBox(10, new Label("Search (Reg No / Course Code):"), searchRegNo, btnView);
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
        setupAutocomplete(searchRegNo, () -> {
            List<String> combined = new ArrayList<>(studentDict);
            combined.addAll(courseDict);
            return combined;
        });
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
        String[] probationReq = {"probationId", "studentRegNos", "startDate", "endDate", "reason"};

        Button btnImportCourses = new Button("Import Courses");
        btnImportCourses.setOnAction(e -> handleSingleImport("Courses", coursesReq, importExportService::importCoursesFile));

        Button btnImportStudents = new Button("Import Students");
        btnImportStudents.setOnAction(e -> handleSingleImport("Students", studentsReq, importExportService::importStudentsFile));

        Button btnImportInstructors = new Button("Import Instructors");
        btnImportInstructors.setOnAction(e -> handleSingleImport("Instructors", instReq, importExportService::importInstructorsFile));

        Button btnImportEnrollments = new Button("Import Enrollments");
        btnImportEnrollments.setOnAction(e -> handleSingleImport("Enrollments", enrollReq, importExportService::importEnrollmentsFile));

        Button btnImportProbation = new Button("Import Probation");
        btnImportProbation.setOnAction(e -> handleSingleImport("Probation Reports", probationReq, importExportService::importProbationReportsFile));

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

        HBox row1 = new HBox(10, btnImportCourses, btnImportStudents, btnImportInstructors, btnImportEnrollments, btnImportProbation);
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

    private javafx.scene.Node createPerformanceInsightsView() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // TAB 1: GRADE DISTRIBUTIONS
        Tab tabGrades = new Tab("Grade Distributions", new FontIcon("fas-chart-bar"));
        VBox gradesBox = new VBox(15);
        gradesBox.setPadding(new Insets(15));

        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Course", "Department");
        typeCombo.setValue("Course");

        ComboBox<String> selectCombo = new ComboBox<>();
        selectCombo.setPrefWidth(250);

        Runnable updateSelectCombo = () -> {
            selectCombo.getItems().clear();
            if ("Course".equals(typeCombo.getValue())) {
                runTaskWithProgress("Loading Courses...", () -> {
                    List<Course> courses = courseService.getAllCoursesSortedByCode();
                    List<String> codes = courses.stream().map(c -> c.getCourseCode().getCode()).collect(Collectors.toList());
                    Platform.runLater(() -> selectCombo.getItems().setAll(codes));
                }, null);
            } else {
                runTaskWithProgress("Loading Departments...", () -> {
                    List<String> depts = analyticsService.getAllDepartments();
                    Platform.runLater(() -> selectCombo.getItems().setAll(depts));
                }, null);
            }
        };

        typeCombo.valueProperty().addListener((obs, oldV, newV) -> updateSelectCombo.run());
        updateSelectCombo.run(); // initial load

        Button btnDraw = new Button("Draw Curve", new FontIcon("fas-paint-brush"));
        filterBox.getChildren().addAll(new Label("Filter Type:"), typeCombo, new Label("Select:"), selectCombo, btnDraw);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Letter Grade");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count of Students");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Grade Distribution Curve");
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);

        btnDraw.setOnAction(e -> {
            String selected = selectCombo.getValue();
            if (selected == null || selected.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please make a selection first.");
                return;
            }

            runTaskWithProgress("Plotting Grade Distribution...", () -> {
                java.util.Map<String, Integer> dist;
                if ("Course".equals(typeCombo.getValue())) {
                    dist = analyticsService.getGradeDistributionForCourse(new CourseCode(selected));
                } else {
                    dist = analyticsService.getGradeDistributionForDepartment(selected);
                }

                Platform.runLater(() -> {
                    barChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName(selected);
                    for (java.util.Map.Entry<String, Integer> entry : dist.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    barChart.getData().add(series);
                    barChart.setTitle("Grade Distribution for: " + selected);
                });
            }, null);
        });

        gradesBox.getChildren().addAll(filterBox, barChart);
        VBox.setVgrow(barChart, Priority.ALWAYS);
        tabGrades.setContent(gradesBox);

        // TAB 2: INSTRUCTOR PERFORMANCE
        Tab tabFaculty = new Tab("Faculty Performance", new FontIcon("fas-chalkboard-teacher"));
        VBox facultyBox = new VBox(15);
        facultyBox.setPadding(new Insets(15));

        Label facultyDesc = new Label("Instructor Performance Insights: Shows average GPA scored by students in their classes.");
        facultyDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");

        TableView<AnalyticsService.InstructorAnalytics> facultyTable = new TableView<>();

        TableColumn<AnalyticsService.InstructorAnalytics, String> colFid = new TableColumn<>("FiD");
        colFid.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFid()));

        TableColumn<AnalyticsService.InstructorAnalytics, String> colName = new TableColumn<>("Instructor Name");
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));

        TableColumn<AnalyticsService.InstructorAnalytics, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDepartment()));

        TableColumn<AnalyticsService.InstructorAnalytics, String> colAvgGpa = new TableColumn<>("Avg Student GPA");
        colAvgGpa.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().getAverageGrade())));

        TableColumn<AnalyticsService.InstructorAnalytics, Integer> colGraded = new TableColumn<>("Graded Students");
        colGraded.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getGradedStudents()));

        facultyTable.getColumns().addAll(colFid, colName, colDept, colAvgGpa, colGraded);
        facultyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button btnRefreshFaculty = new Button("Refresh Faculty Insights", new FontIcon("fas-sync"));
        Runnable loadFaculty = () -> {
            runTaskWithProgress("Loading Faculty Performance...", () -> {
                List<AnalyticsService.InstructorAnalytics> metrics = analyticsService.getInstructorAnalytics();
                Platform.runLater(() -> facultyTable.getItems().setAll(metrics));
            }, null);
        };
        btnRefreshFaculty.setOnAction(e -> loadFaculty.run());

        facultyBox.getChildren().addAll(facultyDesc, facultyTable, btnRefreshFaculty);
        VBox.setVgrow(facultyTable, Priority.ALWAYS);
        tabFaculty.setContent(facultyBox);

        // TAB 3: COURSE POPULARITY & DROPS
        Tab tabPopularity = new Tab("Course Popularity & Drops", new FontIcon("fas-chart-line"));
        VBox popBox = new VBox(15);
        popBox.setPadding(new Insets(15));

        Label popDesc = new Label("Course Demand & Retention: Shows active enrollments vs dropout rates.");
        popDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");

        TableView<AnalyticsService.CoursePopularityAnalytics> popTable = new TableView<>();

        TableColumn<AnalyticsService.CoursePopularityAnalytics, String> colCode = new TableColumn<>("Course Code");
        colCode.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCourseCode()));

        TableColumn<AnalyticsService.CoursePopularityAnalytics, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTitle()));

        TableColumn<AnalyticsService.CoursePopularityAnalytics, String> colPopDept = new TableColumn<>("Department");
        colPopDept.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDepartment()));

        TableColumn<AnalyticsService.CoursePopularityAnalytics, Integer> colActive = new TableColumn<>("Active Enrollments");
        colActive.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getActiveCount()));

        TableColumn<AnalyticsService.CoursePopularityAnalytics, Integer> colDropped = new TableColumn<>("Dropped Enrollments");
        colDropped.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDroppedCount()));

        TableColumn<AnalyticsService.CoursePopularityAnalytics, String> colDropRate = new TableColumn<>("Dropout Rate (%)");
        colDropRate.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.format("%.1f%%", c.getValue().getDropoutRate())));

        popTable.getColumns().addAll(colCode, colTitle, colPopDept, colActive, colDropped, colDropRate);
        popTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button btnRefreshPop = new Button("Refresh Course Popularity", new FontIcon("fas-sync"));
        Runnable loadPop = () -> {
            runTaskWithProgress("Loading Course Popularity...", () -> {
                List<AnalyticsService.CoursePopularityAnalytics> popularityList = analyticsService.getCoursePopularity();
                Platform.runLater(() -> popTable.getItems().setAll(popularityList));
            }, null);
        };
        btnRefreshPop.setOnAction(e -> loadPop.run());

        popBox.getChildren().addAll(popDesc, popTable, btnRefreshPop);
        VBox.setVgrow(popTable, Priority.ALWAYS);
        tabPopularity.setContent(popBox);

        tabPane.getTabs().addAll(tabGrades, tabFaculty, tabPopularity);
        layout.setCenter(tabPane);

        // Preload analytics lists
        loadFaculty.run();
        loadPop.run();

        return layout;
    }

    private void handleAddProbationReport(List<Student> selectedStudents, Runnable refreshTable) {
        Dialog<ProbationReport> dialog = new Dialog<>();
        dialog.setTitle("Add Probation Report");
        dialog.setHeaderText("Put selected student(s) on Probation using Probationary ID");

        ButtonType submitButtonType = new ButtonType("Put on Probation", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField probationIdField = new TextField("PROB-" + (System.currentTimeMillis() % 10000));
        
        String initialRegNos = selectedStudents.stream()
            .map(Student::getRegNo)
            .collect(Collectors.joining(", "));
        TextField studentRegNosField = new TextField(initialRegNos);
        studentRegNosField.setPromptText("Enter comma-separated Registration Numbers...");
        studentRegNosField.setPrefWidth(300);

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusMonths(6));
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Enter the reason for probation...");
        reasonArea.setPrefRowCount(3);

        grid.add(new Label("Probation ID:"), 0, 0);
        grid.add(probationIdField, 1, 0);
        grid.add(new Label("Student Reg Nos:"), 0, 1);
        grid.add(studentRegNosField, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("Reason:"), 0, 4);
        grid.add(reasonArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node submitButton = dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(reasonArea.getText().trim().isEmpty());

        Runnable validation = () -> {
            boolean hasId = !probationIdField.getText().trim().isEmpty();
            boolean hasRegs = !studentRegNosField.getText().trim().isEmpty();
            boolean hasReason = !reasonArea.getText().trim().isEmpty();
            boolean hasDates = startDatePicker.getValue() != null && endDatePicker.getValue() != null;
            submitButton.setDisable(!(hasId && hasRegs && hasReason && hasDates));
        };

        probationIdField.textProperty().addListener((o, oldVal, newVal) -> validation.run());
        studentRegNosField.textProperty().addListener((o, oldVal, newVal) -> validation.run());
        reasonArea.textProperty().addListener((o, oldVal, newVal) -> validation.run());
        startDatePicker.valueProperty().addListener((o, oldVal, newVal) -> validation.run());
        endDatePicker.valueProperty().addListener((o, oldVal, newVal) -> validation.run());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                String regNosStr = studentRegNosField.getText().trim();
                List<String> regNos = java.util.Arrays.stream(regNosStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

                return new ProbationReport(
                    probationIdField.getText().trim(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    reasonArea.getText().trim(),
                    regNos
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(report -> {
            if (report.getEndDate().isBefore(report.getStartDate())) {
                showAlert(Alert.AlertType.ERROR, "End Date cannot be before Start Date.");
                return;
            }
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Validate all students exist
                    for (String regNo : report.getStudentRegNos()) {
                        updateMessage("Validating student " + regNo + "...");
                        studentService.findStudentByRegNo(regNo);
                    }
                    
                    updateMessage("Adding probation report...");
                    probationService.addProbationReport(report);
                    return null;
                }
            };

            runJavaFXTask(task, "Processing Probation Report...", () -> {
                showAlert(Alert.AlertType.INFORMATION, "Student(s) successfully placed on probation under report " + report.getProbationId());
            });
        });
    }

    private javafx.scene.Node createCohortGradingView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Label title = new Label("Cohort Relative Grading & Marks Upload");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label desc = new Label(
            "Upload a CSV file containing student component marks. The system will calculate the grand total marks (20% assignments, 5% attendance, 15% labs, 20% mids, 40% endterms), update the student records, and dynamically calculate the relative grades for the affected cohorts."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -color-fg-muted;");

        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button btnUpload = new Button("Upload Marks CSV", new FontIcon("fas-upload"));
        btnUpload.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");

        Button btnDownloadTemplate = new Button("Download CSV Template", new FontIcon("fas-download"));
        btnDownloadTemplate.getStyleClass().add("flat");

        actionBox.getChildren().addAll(btnUpload, btnDownloadTemplate);

        Label tableLabel = new Label("CSV Data Preview");
        tableLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TableView<String[]> previewTable = new TableView<>();
        previewTable.setPlaceholder(new Label("No CSV file uploaded yet."));
        previewTable.setMinHeight(250);
        previewTable.setPrefHeight(300);
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label logLabel = new Label("Processing Logs");
        logLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setPromptText("Processing logs will be shown here.");
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        Button btnProcess = new Button("Process & Recalculate Grades", new FontIcon("fas-play"));
        btnProcess.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-font-weight: bold;");
        btnProcess.setDisable(true);

        final List<String[]>[] loadedRows = new List[1];
        final File[] selectedFile = new File[1];

        String[] requiredFields = {"student_reg_no", "course_code", "enrollment_year", "enrollment_semester", "assignments", "attendance", "labs", "mids", "endterms"};

        btnDownloadTemplate.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save CSV Template");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            chooser.setInitialFileName("marks_template.csv");
            File file = chooser.showSaveDialog(mainLayout.getScene().getWindow());
            if (file != null) {
                try {
                    Files.writeString(file.toPath(), String.join(",", requiredFields) + "\n" +
                        "S001,CS101,2026,WINTER,80,100,90,75,85\n" +
                        "S002,CS101,2026,WINTER,70,90,80,60,75\n");
                    showAlert(Alert.AlertType.INFORMATION, "Template saved successfully to " + file.getName());
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Failed to save template: " + ex.getMessage());
                }
            }
        });

        btnUpload.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Marks CSV File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = chooser.showOpenDialog(mainLayout.getScene().getWindow());
            if (file != null) {
                if (!verifyHeader(file.toPath(), requiredFields)) {
                    showAlert(Alert.AlertType.ERROR, "Invalid CSV format.\nExpected header fields (case-insensitive):\n" + String.join(", ", requiredFields));
                    return;
                }
                try {
                    List<String[]> rows = parseCsv(file.toPath());
                    if (rows.isEmpty() || rows.size() <= 1) {
                        showAlert(Alert.AlertType.ERROR, "The selected CSV file has no records.");
                        return;
                    }
                    loadedRows[0] = rows;
                    selectedFile[0] = file;

                    previewTable.getColumns().clear();
                    String[] headers = rows.get(0);
                    for (int i = 0; i < headers.length; i++) {
                        final int colIndex = i;
                        TableColumn<String[], String> column = new TableColumn<>(headers[i]);
                        column.setCellValueFactory(cd -> {
                            String[] rowData = cd.getValue();
                            String val = (rowData != null && colIndex < rowData.length) ? rowData[colIndex] : "";
                            return new ReadOnlyStringWrapper(val);
                        });
                        previewTable.getColumns().add(column);
                    }

                    ObservableList<String[]> data = FXCollections.observableArrayList();
                    for (int i = 1; i < rows.size(); i++) {
                        data.add(rows.get(i));
                    }
                    previewTable.setItems(data);
                    btnProcess.setDisable(false);
                    logArea.appendText("Loaded CSV: " + file.getName() + " containing " + (rows.size() - 1) + " records.\n");

                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Failed to read CSV: " + ex.getMessage());
                }
            }
        });

        btnProcess.setOnAction(e -> {
            if (loadedRows[0] == null || selectedFile[0] == null) return;
            
            logArea.clear();
            logArea.appendText("Starting relative grading calculation...\n");
            btnProcess.setDisable(true);
            btnUpload.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<String[]> rows = loadedRows[0];
                    String[] headers = rows.get(0);

                    int regIndex = -1, courseIndex = -1, yearIndex = -1, semIndex = -1;
                    int assignmentsIndex = -1, attendanceIndex = -1, labsIndex = -1, midsIndex = -1, endtermsIndex = -1;
                    for (int i = 0; i < headers.length; i++) {
                        String h = headers[i].trim().toLowerCase();
                        if (h.equals("student_reg_no")) regIndex = i;
                        else if (h.equals("course_code")) courseIndex = i;
                        else if (h.equals("enrollment_year")) yearIndex = i;
                        else if (h.equals("enrollment_semester")) semIndex = i;
                        else if (h.equals("assignments")) assignmentsIndex = i;
                        else if (h.equals("attendance")) attendanceIndex = i;
                        else if (h.equals("labs")) labsIndex = i;
                        else if (h.equals("mids")) midsIndex = i;
                        else if (h.equals("endterms")) endtermsIndex = i;
                    }

                    if (regIndex == -1 || courseIndex == -1 || yearIndex == -1 || semIndex == -1 ||
                        assignmentsIndex == -1 || attendanceIndex == -1 || labsIndex == -1 || midsIndex == -1 || endtermsIndex == -1) {
                        throw new Exception("Missing required columns in CSV header.");
                    }

                    int total = rows.size() - 1;
                    int successCount = 0;
                    int errorCount = 0;

                    java.util.Set<String> uniqueCohorts = new java.util.LinkedHashSet<>();

                    for (int i = 1; i < rows.size(); i++) {
                        String[] row = rows.get(i);
                        if (row.length < headers.length) {
                            final String skipMsg = "Row " + i + ": Incomplete data. Skipping.\n";
                            Platform.runLater(() -> logArea.appendText(skipMsg));
                            errorCount++;
                            continue;
                        }

                        String regNo = row[regIndex].trim();
                        String courseStr = row[courseIndex].trim();
                        String yearStr = row[yearIndex].trim();
                        String semStr = row[semIndex].trim();
                        String assignmentsStr = row[assignmentsIndex].trim();
                        String attendanceStr = row[attendanceIndex].trim();
                        String labsStr = row[labsIndex].trim();
                        String midsStr = row[midsIndex].trim();
                        String endtermsStr = row[endtermsIndex].trim();

                        updateMessage("Processing student " + regNo + " (" + i + "/" + total + ")...");
                        updateProgress(i, total);

                        try {
                            int year = Integer.parseInt(yearStr);
                            double assignments = Double.parseDouble(assignmentsStr);
                            double attendance = Double.parseDouble(attendanceStr);
                            double labs = Double.parseDouble(labsStr);
                            double mids = Double.parseDouble(midsStr);
                            double endterms = Double.parseDouble(endtermsStr);

                            double grandTotal = (assignments * 0.20) + (attendance * 0.05) + (labs * 0.15) + (mids * 0.20) + (endterms * 0.40);
                            CourseCode courseCode = new CourseCode(courseStr);

                            enrollmentService.updateStudentMarks(regNo, courseCode, year, semStr, grandTotal);
                            final String successMsg = String.format("Row %d: Updated student %s with calculated grand total %.2f (assignments=%.1f, attendance=%.1f, labs=%.1f, mids=%.1f, endterms=%.1f).\n",
                                    i, regNo, grandTotal, assignments, attendance, labs, mids, endterms);
                            Platform.runLater(() -> logArea.appendText(successMsg));

                            uniqueCohorts.add(courseStr + "," + year + "," + semStr);
                            successCount++;

                        } catch (NumberFormatException nfe) {
                            final String numErrMsg = "Row " + i + ": Invalid numeric values in row. Skipping.\n";
                            Platform.runLater(() -> logArea.appendText(numErrMsg));
                            errorCount++;
                        } catch (RecordNotFoundException rnfe) {
                            final String notFoundMsg = "Row " + i + ": Enrollment record not found for student " + regNo + " in " + courseStr + " - " + semStr + " " + yearStr + ". Skipping.\n";
                            Platform.runLater(() -> logArea.appendText(notFoundMsg));
                            errorCount++;
                        } catch (IllegalStateException ise) {
                            final String iseMsg = "Row " + i + ": Error - Marked data cannot be overwritten without proper change in marks for student " + regNo + " in " + courseStr + ". Skipping.\n";
                            Platform.runLater(() -> logArea.appendText(iseMsg));
                            errorCount++;
                        } catch (Exception ex) {
                            final String exMsg = "Row " + i + ": Error - " + ex.getMessage() + ". Skipping.\n";
                            Platform.runLater(() -> logArea.appendText(exMsg));
                            errorCount++;
                        }
                    }

                    Platform.runLater(() -> logArea.appendText("\nRecalculating cohort relative grading...\n"));

                    int cohortIndex = 0;
                    for (String cohort : uniqueCohorts) {
                        String[] parts = cohort.split(",");
                        String courseCodeStr = parts[0];
                        int year = Integer.parseInt(parts[1]);
                        String sem = parts[2];

                        updateMessage("Recalculating grades for cohort " + courseCodeStr + " (" + (cohortIndex + 1) + "/" + uniqueCohorts.size() + ")...");
                        try {
                            enrollmentService.calculateRelativeGrading(new CourseCode(courseCodeStr), year, sem);
                            final String cohortMsg = "Cohort " + courseCodeStr + " (" + sem + " " + year + "): Grades successfully recalculated.\n";
                            Platform.runLater(() -> logArea.appendText(cohortMsg));
                        } catch (Exception ex) {
                            final String cohortErrMsg = "Cohort " + courseCodeStr + " (" + sem + " " + year + "): Calculation error - " + ex.getMessage() + "\n";
                            Platform.runLater(() -> logArea.appendText(cohortErrMsg));
                        }
                        cohortIndex++;
                    }

                    final String summary = String.format("\nProcessing complete! Success: %d rows updated, Errors/Warnings: %d. Recalculated %d cohorts.\n",
                            successCount, errorCount, uniqueCohorts.size());
                    Platform.runLater(() -> logArea.appendText(summary));

                    return null;
                }
            };

            runJavaFXTask(task, "Processing Marks & Recalculating Grades...", () -> {
                btnUpload.setDisable(false);
                showAlert(Alert.AlertType.INFORMATION, "Marks processed and grades recalculated successfully!");
            });
        });

        layout.getChildren().addAll(
            title,
            desc,
            actionBox,
            tableLabel,
            previewTable,
            btnProcess,
            logLabel,
            logArea
        );
        return layout;
    }
}
