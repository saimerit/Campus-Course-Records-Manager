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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        HBox statusBar = new HBox(10, globalProgressIndicator, globalStatusLabel);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white;");

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

        root.setStyle("-fx-base: #263238; -fx-background: #263238; -fx-control-inner-background: #37474F; -fx-accent: #00B0FF; -fx-text-background-color: white;");

        Scene scene = new Scene(root, 1100, 750);
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

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.execute();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            globalProgressIndicator.setVisible(false);
            globalStatusLabel.setText("Ready");
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            globalProgressIndicator.setVisible(false);
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

        form.addRow(0, new Label("Student ID:"), idField, new Label("Reg No:"), regNoField);
        form.addRow(1, new Label("First Name:"), firstNameField, new Label("Last Name:"), lastNameField);
        form.addRow(2, new Label("Email:"), emailField, new Label("Phone:"), phoneField);
        form.addRow(3, new Label("DOB:"), dobPicker);

        Button btnAdd = new Button("Add Student");
        form.add(btnAdd, 0, 4, 2, 1);

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
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));
        Button btnRefresh = new Button("Refresh");
        btnRefresh.setOnAction(e -> refreshTable.run());

        Button btnTranscript = new Button("View Transcript");
        btnTranscript.setOnAction(e -> {
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
        });

        Button btnUpdateStatus = new Button("Update Status");
        btnUpdateStatus.setOnAction(e -> {
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
        });

        actions.getChildren().addAll(btnRefresh, btnTranscript, btnUpdateStatus);

        HBox searchBox = new HBox(10, new Label("Search:"), searchField);
        searchBox.setPadding(new Insets(10, 0, 10, 0));

        VBox topBox = new VBox(10, new Label("Add New Student"), form, searchBox);
        layout.setTop(topBox);
        layout.setCenter(table);
        layout.setBottom(actions);

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

        form.addRow(0, new Label("Instructor FiD:"), fidField, new Label("Department:"), deptField);
        form.addRow(1, new Label("First Name:"), firstNameField, new Label("Last Name:"), lastNameField);
        form.addRow(2, new Label("Email:"), emailField, new Label("Phone:"), phoneField);
        form.addRow(3, new Label("DOB:"), dobPicker, new Label("Cabin No:"), cabinField);

        Button btnAdd = new Button("Add Instructor");
        form.add(btnAdd, 0, 4, 2, 1);

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

        Runnable refreshTable = () -> {
            runTaskWithProgress("Loading Instructors...", () -> {
                List<Instructor> list = instructorService.getAllInstructorsSortedById();
                Platform.runLater(() -> table.getItems().setAll(list));
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

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));
        Button btnRefresh = new Button("Refresh");
        btnRefresh.setOnAction(e -> refreshTable.run());
        actions.getChildren().addAll(btnRefresh);

        VBox topBox = new VBox(10, new Label("Add New Instructor"), form);
        layout.setTop(topBox);
        layout.setCenter(table);
        layout.setBottom(actions);
        
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

        form.addRow(0, new Label("Code:"), codeField, new Label("Title:"), titleField);
        form.addRow(1, new Label("Credits:"), creditsField, new Label("Department:"), deptField);
        form.addRow(2, new Label("Semester:"), semesterCombo, new Label("Classroom:"), classroomField);

        Button btnAdd = new Button("Add Course");
        form.add(btnAdd, 0, 3, 2, 1);

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

        Runnable refreshTable = () -> {
            runTaskWithProgress("Loading Courses...", () -> {
                List<Course> list = courseService.getAllCoursesSortedByCode();
                Platform.runLater(() -> table.getItems().setAll(list));
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

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));
        Button btnRefresh = new Button("Refresh");
        btnRefresh.setOnAction(e -> refreshTable.run());

        Button btnAssign = new Button("Assign Instructor");
        btnAssign.setOnAction(e -> {
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
        });

        actions.getChildren().addAll(btnRefresh, btnAssign);

        VBox topBox = new VBox(10, new Label("Add New Course"), form);
        layout.setTop(topBox);
        layout.setCenter(table);
        layout.setBottom(actions);
        
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

        Button btnEnroll = new Button("Enroll");
        Button btnUnenroll = new Button("Unenroll");
        Button btnRecordGrade = new Button("Record Grade");

        HBox formActions = new HBox(10, btnEnroll, btnUnenroll, btnRecordGrade);
        form.add(formActions, 0, 3, 2, 1);

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

        VBox rightSide = new VBox(10);
        TextField searchRegNo = new TextField();
        searchRegNo.setPromptText("Enter Reg No to View Enrollments");
        Button btnView = new Button("View Enrollments");
        btnView.setOnAction(e -> {
            runTaskWithProgress("Fetching Enrollments...", () -> {
                // Thread.sleep(2000) simulation of heavy operation (kept from original requirement)
                Thread.sleep(2000); 
                List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(searchRegNo.getText());
                Platform.runLater(() -> table.getItems().setAll(enrollments));
            }, null);
        });
        rightSide.getChildren().addAll(new Label("View Enrollments For Student"), searchRegNo, btnView, table);
        rightSide.setPrefWidth(500);

        HBox split = new HBox(20, form, rightSide);

        layout.setCenter(split);
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
        
        Button btnDeleteDb = new Button("Delete Database (DANGER)");
        btnDeleteDb.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #990000;");
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
}
