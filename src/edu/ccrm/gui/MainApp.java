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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
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
        centerSpinner.setVisible(false);

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

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createStudentsTab());
        tabPane.getTabs().add(createInstructorsTab());
        tabPane.getTabs().add(createCoursesTab());
        tabPane.getTabs().add(createEnrollmentsTab());
        tabPane.getTabs().add(createFileOperationsTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        StackPane appRoot = new StackPane();
        appRoot.getChildren().addAll(root, dimOverlay, centerSpinner);
        
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Scene scene = new Scene(appRoot, 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize Database in background
        runTaskWithProgress("Initializing Database...", () -> {
            DatabaseInitializer.initialize();
        }, () -> showAlert(Alert.AlertType.INFORMATION, "Database initialized successfully."));
    }

    private void runTaskWithProgress(String message, TaskAction action, Runnable onSuccess) {
        globalProgressIndicator.setVisible(true);
        globalStatusLabel.setText(message);
        dimOverlay.setVisible(true);
        centerSpinner.setVisible(true);

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
            centerSpinner.setVisible(false);
            globalStatusLabel.setText("Ready");
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            globalProgressIndicator.setVisible(false);
            dimOverlay.setVisible(false);
            centerSpinner.setVisible(false);
            globalStatusLabel.setText("Error occurred");
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        new Thread(task).start();
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
    private Tab createStudentsTab() {
        Tab tab = new Tab("Students");
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

        form.addRow(0, new Label("Student ID:"), idField);
        form.addRow(1, new Label("Reg No:"), regNoField);
        form.addRow(2, new Label("First Name:"), firstNameField);
        form.addRow(3, new Label("Last Name:"), lastNameField);
        form.addRow(4, new Label("Email:"), emailField);
        form.addRow(5, new Label("Phone:"), phoneField);
        form.addRow(6, new Label("DOB:"), dobPicker);

        Button btnAdd = new Button("Add Student", new FontIcon("fas-user-plus"));
        form.add(btnAdd, 0, 7, 2, 1);
        
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
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    } else if ("PROBATION".equals(item)) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colId, colRegNo, colName, colEmail, colDob, colPhone, colStatus);

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
                Student s = new Student(id, regNoField.getText(), new Name(firstNameField.getText(), lastNameField.getText()), emailField.getText(), dobPicker.getValue(), phoneField.getText());
                studentService.addStudent(s);
                showAlert(Alert.AlertType.INFORMATION, "Student added successfully.");
                refreshTable.run();
                idField.clear(); regNoField.clear(); firstNameField.clear(); lastNameField.clear(); emailField.clear(); phoneField.clear(); dobPicker.setValue(null);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to add student: " + ex.getMessage());
            }
        });

        // Bottom Actions
        Button btnRefresh = new Button("Refresh", new FontIcon("fas-sync"));
        btnRefresh.setOnAction(e -> refreshTable.run());

        Runnable transcriptAction = () -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select a student first.");
                return;
            }
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
                        TableColumn<Enrollment, String> cGrade = new TableColumn<>("Grade");
                        cGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade() != null ? c.getValue().getGrade().toString() : "N/A"));
                        
                        coursesTable.getColumns().addAll(cCode, cTitle, cCredits, cGrade);
                        coursesTable.getItems().setAll(enrollments);
                        coursesTable.setPrefHeight(200);

                        Label cgpaLabel = new Label(String.format("CGPA: %.2f", cgpa));
                        cgpaLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        cgpaLabel.setStyle("-fx-text-fill: white;");

                        content.getChildren().addAll(headerInfo, coursesTable, cgpaLabel);
                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Failed to load transcript: " + ex.getMessage()));
                }
            }, null);
        };

        Runnable updateStatusAction = () -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select a student first.");
                return;
            }
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
            viewTranscript.setOnAction(e -> transcriptAction.run());
            
            MenuItem changeStatus = new MenuItem("Change Status");
            changeStatus.setGraphic(new FontIcon("fas-edit"));
            changeStatus.setOnAction(e -> updateStatusAction.run());
            
            contextMenu.getItems().addAll(viewTranscript, changeStatus);
            row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty())
                .then((ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });

        HBox topBox = new HBox(10, new Label("Search:"), searchField, btnRefresh);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        
        Button btnViewTranscript = new Button("View Transcript", new FontIcon("fas-file-invoice"));
        btnViewTranscript.setOnAction(e -> transcriptAction.run());
        
        Button btnChangeStatus = new Button("Change Status", new FontIcon("fas-edit"));
        btnChangeStatus.setOnAction(e -> updateStatusAction.run());
        
        HBox bottomBox = new HBox(10, btnViewTranscript, btnChangeStatus);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        VBox rightPane = new VBox(topBox, table, bottomBox);
        rightPane.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.33);

        layout.setCenter(splitPane);

        tab.setContent(layout);
        // Refresh on load
        tab.setOnSelectionChanged(e -> {
            if(tab.isSelected()) refreshTable.run();
        });
        return tab;
    }

    // =========================================================================
    // INSTRUCTORS TAB
    // =========================================================================
    private Tab createInstructorsTab() {
        Tab tab = new Tab("Instructors");
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

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.35);

        layout.setCenter(splitPane);
        
        tab.setContent(layout);
        tab.setOnSelectionChanged(e -> {
            if(tab.isSelected()) refreshTable.run();
        });
        return tab;
    }

    // =========================================================================
    // COURSES TAB
    // =========================================================================
    private Tab createCoursesTab() {
        Tab tab = new Tab("Courses");
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

        Runnable assignAction = () -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if(selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select a course first.");
                return;
            }
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
            assignMenu.setOnAction(e -> assignAction.run());
            contextMenu.getItems().add(assignMenu);
            row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty())
                .then((ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });

        HBox topBox = new HBox(10, new Label("Search:"), searchField, btnRefresh);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        
        Button btnAssign = new Button("Assign Instructor", new FontIcon("fas-chalkboard-teacher"));
        btnAssign.setOnAction(e -> assignAction.run());
        
        HBox bottomBox = new HBox(10, btnAssign);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        VBox rightPane = new VBox(topBox, table, bottomBox);
        rightPane.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.33);

        layout.setCenter(splitPane);
        
        tab.setContent(layout);
        tab.setOnSelectionChanged(e -> {
            if(tab.isSelected()) refreshTable.run();
        });
        return tab;
    }

    // =========================================================================
    // ENROLLMENTS TAB
    // =========================================================================
    private Tab createEnrollmentsTab() {
        Tab tab = new Tab("Enrollments");
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
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.33);

        layout.setCenter(splitPane);
        
        List<String> studentDict = new ArrayList<>();
        List<String> courseDict = new ArrayList<>();

        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
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
            }
        });

        setupAutocomplete(regNoField, () -> studentDict);
        setupAutocomplete(searchRegNo, () -> studentDict);
        setupAutocomplete(courseCodeField, () -> courseDict);
        
        tab.setContent(layout);
        return tab;
    }

    // =========================================================================
    // FILE OPERATIONS TAB
    // =========================================================================
    private Tab createFileOperationsTab() {
        Tab tab = new Tab("File & System Ops");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Button btnImportCourses = new Button("Import Courses");
        btnImportCourses.setOnAction(e -> runTaskWithProgress("Importing Courses...", 
            importExportService::importCoursesFromTestData, () -> showAlert(Alert.AlertType.INFORMATION, "Courses imported.")));

        Button btnImportStudents = new Button("Import Students");
        btnImportStudents.setOnAction(e -> runTaskWithProgress("Importing Students...", 
            importExportService::importStudentsFromTestData, () -> showAlert(Alert.AlertType.INFORMATION, "Students imported.")));

        Button btnImportInstructors = new Button("Import Instructors");
        btnImportInstructors.setOnAction(e -> runTaskWithProgress("Importing Instructors...", 
            importExportService::importInstructorsFromTestData, () -> showAlert(Alert.AlertType.INFORMATION, "Instructors imported.")));

        Button btnImportEnrollments = new Button("Import Enrollments");
        btnImportEnrollments.setOnAction(e -> runTaskWithProgress("Importing Enrollments...", 
            importExportService::importEnrollmentsFromTestData, () -> showAlert(Alert.AlertType.INFORMATION, "Enrollments imported.")));

        Button btnImportAll = new Button("Import All");
        btnImportAll.setOnAction(e -> runTaskWithProgress("Importing All Data...", () -> {
            importExportService.importInstructorsFromTestData();
            importExportService.importStudentsFromTestData();
            importExportService.importCoursesFromTestData();
            Platform.runLater(() -> globalStatusLabel.setText("Waiting 5s for consistency..."));
            Thread.sleep(5000);
            importExportService.importEnrollmentsFromTestData();
        }, () -> showAlert(Alert.AlertType.INFORMATION, "All data imported successfully.")));

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
        
        Button btnDeleteDb = new Button("Delete Database (DANGER)", new FontIcon("fas-trash"));
        btnDeleteDb.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDeleteDb.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Database");
            alert.setHeaderText("ARE YOU SURE?");
            alert.setContentText("This will drop all tables and cannot be undone. You will need to restart the application.");
            alert.showAndWait().ifPresent(response -> {
                if(response == ButtonType.OK) {
                    runTaskWithProgress("Deleting database...", () -> dbAdminService.dropAllTables(), 
                        () -> showAlert(Alert.AlertType.INFORMATION, "Database deleted. Please restart."));
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

        tab.setContent(layout);
        return tab;
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
