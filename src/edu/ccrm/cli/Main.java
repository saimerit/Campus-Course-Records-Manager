package edu.ccrm.cli;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.DuplicateEnrollmentException;
import edu.ccrm.exception.MaxCreditLimitExceededException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.BackupService;
import edu.ccrm.io.DatabaseInitializer;
import edu.ccrm.io.ImportExportService;
import edu.ccrm.service.*;
import edu.ccrm.util.RecursiveUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    private static final StudentService studentService = new StudentService();
    private static final InstructorService instructorService = new InstructorService();
    private static final CourseService courseService = new CourseService(instructorService);
    private static final EnrollmentService enrollmentService = new EnrollmentService(studentService, courseService);
    private static final TranscriptService transcriptService = new TranscriptService(studentService, enrollmentService);
    private static final ImportExportService importExportService = new ImportExportService(studentService, instructorService, courseService, enrollmentService);
    private static final BackupService backupService = new BackupService();
    private static final DatabaseAdminService dbAdminService = new DatabaseAdminService();

    public static void main(String[] args) {
        System.out.println("Welcome to Campus Course & Records Manager (CCRM)");
        DatabaseInitializer.initialize();
        printJavaPlatformInfo();
        mainMenuLoop:
        while (true) {
            printMainMenu();
            int choice = getUserIntInput("Enter your choice: ");
            switch (choice) {
                case 1:
                    manageStudents();
                    break;
                case 2:
                    manageInstructors();
                    break;
                case 3:
                    manageCourses();
                    break;
                case 4:
                    manageEnrollments();
                    break;
                case 5:
                    manageFileOperations();
                    break;
                case 6:
                    deleteDatabaseAndExit();
                    break mainMenuLoop;
                case 7:
                    System.out.println("Exiting application.");
                    break mainMenuLoop;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Manage Students");
        System.out.println("2. Manage Instructors");
        System.out.println("3. Manage Courses");
        System.out.println("4. Manage Enrollments & Grades");
        System.out.println("5. File Operations");
        System.out.println("6. Delete Database & Exit");
        System.out.println("7. Exit");
    }

    private static void deleteDatabaseAndExit() {
        System.out.println("\n--- Delete Database ---");
        System.out.print("ARE YOU SURE you want to delete all data? This cannot be undone. (YES/NO): ");
        String confirmation = scanner.nextLine();

        if (confirmation.equalsIgnoreCase("YES")) {
            System.out.println("Proceeding with database deletion...");
            dbAdminService.dropAllTables();
            System.out.println("Database deleted. Please restart the application.");
        } else {
            System.out.println("Database deletion cancelled.");
        }
    }

    private static void manageStudents() {
        while (true) {
            System.out.println("\n--- Student Management ---");
            System.out.println("1. Add New Student");
            System.out.println("2. List All Students");
            System.out.println("3. View Student Profile & Transcript");
            System.out.println("4. Update Student Status");
            System.out.println("5. Update Student Details");
            System.out.println("6. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1:
                    addStudent();
                    break;
                case 2:
                    listAllStudents();
                    break;
                case 3:
                    viewStudentTranscript();
                    break;
                case 4:
                    updateStudentStatus();
                    break;
                case 5:
                    updateStudentDetails();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void manageInstructors() {
        while (true) {
            System.out.println("\n--- Instructor Management ---");
            System.out.println("1. Add New Instructor");
            System.out.println("2. List All Instructors");
            System.out.println("3. Update Instructor Details");
            System.out.println("4. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1:
                    addInstructor();
                    break;
                case 2:
                    listAllInstructors();
                    break;
                case 3:
                    updateInstructorDetails();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void manageCourses() {
        while (true) {
            System.out.println("\n--- Course Management ---");
            System.out.println("1. Add New Course");
            System.out.println("2. List All Courses");
            System.out.println("3. Search & Filter Courses");
            System.out.println("4. Assign Instructor to Course");
            System.out.println("5. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1:
                    addCourse();
                    break;
                case 2:
                    listAllCourses();
                    break;
                case 3:
                    searchAndFilterCourses();
                    break;
                case 4:
                    assignInstructorToCourse();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void manageEnrollments() {
        while (true) {
            System.out.println("\n--- Enrollment & Grading ---");
            System.out.println("1. Enroll Student in Course");
            System.out.println("2. Unenroll Student from Course");
            System.out.println("3. Record Student's Grade");
            System.out.println("4. View All Enrollments by Student");
            System.out.println("5. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1:
                    enrollStudent();
                    break;
                case 2:
                    unenrollStudent();
                    break;
                case 3:
                    recordGrade();
                    break;
                case 4:
                    viewAllEnrollmentsByStudent();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void manageFileOperations() {
        while (true) {
            System.out.println("\n--- File Operations ---");
            System.out.println("1. Import Data from CSV files");
            System.out.println("2. Export Data to CSV files");
            System.out.println("3. Create a Backup");
            System.out.println("4. Show Backup Directory Size");
            System.out.println("5. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1:
                    importDataWithOptions();
                    break;
                case 2:
                    exportData();
                    break;
                case 3:
                    createBackup();
                    break;
                case 4:
                    showBackupSize();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void importDataWithOptions() {
        System.out.println("\n--- Import Data from CSV ---");
        System.out.println("1. Import Courses");
        System.out.println("2. Import Students");
        System.out.println("3. Import Instructors");
        System.out.println("4. Import Enrollments");
        System.out.println("5. Import All");
        int choice = getUserIntInput("Enter your choice: ");

        switch (choice) {
            case 1:
                importExportService.importCoursesFromTestData();
                break;
            case 2:
                importExportService.importStudentsFromTestData();
                break;
            case 3:
                importExportService.importInstructorsFromTestData();
                break;
            case 4:
                importExportService.importEnrollmentsFromTestData();
                break;
            case 5:
                importExportService.importInstructorsFromTestData();
                importExportService.importStudentsFromTestData();
                importExportService.importCoursesFromTestData();
                System.out.println("Waiting for 5 seconds before importing enrollments to ensure data consistency...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Delay interrupted: " + e.getMessage());
                }
                importExportService.importEnrollmentsFromTestData();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }


    private static void addStudent() {
        System.out.println("\n--- Add New Student ---");
        try {
            int id = getUserIntInput("Enter Student ID: ");
            System.out.print("Enter Registration Number: ");
            String regNo = scanner.nextLine();
            System.out.print("Enter First Name: ");
            String firstName = scanner.nextLine();
            System.out.print("Enter Last Name: ");
            String lastName = scanner.nextLine();
            System.out.print("Enter Email: ");
            String email = scanner.nextLine();

            Student student = new Student(id, regNo, new Name(firstName, lastName), email);
            studentService.addStudent(student);
            System.out.println("Student added successfully.");
        } catch (DataIntegrityException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void listAllStudents() {
        System.out.println("\n--- All Students ---");
        List<Student> students = studentService.getAllStudentsSortedById();
        if (students.isEmpty()) {
            System.out.println("No students found.");
        } else {
            String[] headers = {"ID", "Reg No", "Name", "Email", "Status"};
            List<String[]> rows = new ArrayList<>();
            for (Student s : students) {
                rows.add(new String[]{
                        String.valueOf(s.getId()),
                        s.getRegNo(),
                        s.getFullName().toString(),
                        s.getEmail(),
                        s.getStatus().toString()
                });
            }
            printTable(headers, rows);
        }
    }

    private static void addInstructor() {
        System.out.println("\n--- Add New Instructor ---");
        try {
            System.out.print("Enter Instructor FiD: ");
            String fId = scanner.nextLine();
            System.out.print("Enter First Name: ");
            String firstName = scanner.nextLine();
            System.out.print("Enter Last Name: ");
            String lastName = scanner.nextLine();
            System.out.print("Enter Email: ");
            String email = scanner.nextLine();
            System.out.print("Enter Department: ");
            String department = scanner.nextLine();

            Instructor instructor = new Instructor(fId, new Name(firstName, lastName), email, department);
            instructorService.addInstructor(instructor);
            System.out.println("Instructor added successfully.");
        } catch (DataIntegrityException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void listAllInstructors() {
        System.out.println("\n--- All Instructors ---");
        List<Instructor> instructors = instructorService.getAllInstructorsSortedById();
        if (instructors.isEmpty()) {
            System.out.println("No instructors found.");
        } else {
            String[] headers = {"FiD", "Name", "Email", "Department"};
            List<String[]> rows = new ArrayList<>();
            for (Instructor i : instructors) {
                rows.add(new String[]{
                        i.getFiD(),
                        i.getFullName().toString(),
                        i.getEmail(),
                        i.getDepartment()
                });
            }
            printTable(headers, rows);
        }
    }

    private static void viewStudentTranscript() {
        System.out.println("\n--- View Student Transcript ---");
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        try {
            String transcript = transcriptService.generateTranscript(regNo);
            System.out.println(transcript);
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void updateStudentStatus() {
        System.out.println("\n--- Update Student Status ---");
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        System.out.println("Available statuses: " + Arrays.toString(Student.Status.values()));
        System.out.print("Enter new status: ");
        String statusStr = scanner.nextLine().toUpperCase();
        try {
            Student.Status newStatus = Student.Status.valueOf(statusStr);
            studentService.updateStudentStatus(regNo, newStatus);
            System.out.println("Status updated successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status provided.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void updateStudentDetails() {
        System.out.println("\n--- Update Student Details ---");
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        try {
            Student student = studentService.findStudentByRegNo(regNo);
            System.out.print("Enter new First Name (or press Enter to keep '" + student.getFullName().getFirstName() + "'): ");
            String firstName = scanner.nextLine();
            if (firstName.isEmpty()) {
                firstName = student.getFullName().getFirstName();
            }

            System.out.print("Enter new Last Name (or press Enter to keep '" + student.getFullName().getLastName() + "'): ");
            String lastName = scanner.nextLine();
            if (lastName.isEmpty()) {
                lastName = student.getFullName().getLastName();
            }

            System.out.print("Enter new Email (or press Enter to keep '" + student.getEmail() + "'): ");
            String email = scanner.nextLine();
            if (email.isEmpty()) {
                email = student.getEmail();
            }

            Student updatedStudent = new Student(student.getId(), regNo, new Name(firstName, lastName), email, student.getStatus(), student.getRegistrationDate());
            studentService.updateStudent(updatedStudent);
            System.out.println("Student details updated successfully.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (DataIntegrityException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void updateInstructorDetails() {
        System.out.println("\n--- Update Instructor Details ---");
        System.out.print("Enter Instructor FiD: ");
        String fId = scanner.nextLine();
        try {
            Instructor instructor = instructorService.findInstructorByFiD(fId);
            System.out.print("Enter new First Name (or press Enter to keep '" + instructor.getFullName().getFirstName() + "'): ");
            String firstName = scanner.nextLine();
            if (firstName.isEmpty()) {
                firstName = instructor.getFullName().getFirstName();
            }

            System.out.print("Enter new Last Name (or press Enter to keep '" + instructor.getFullName().getLastName() + "'): ");
            String lastName = scanner.nextLine();
            if (lastName.isEmpty()) {
                lastName = instructor.getFullName().getLastName();
            }

            System.out.print("Enter new Email (or press Enter to keep '" + instructor.getEmail() + "'): ");
            String email = scanner.nextLine();
            if (email.isEmpty()) {
                email = instructor.getEmail();
            }
            
            System.out.print("Enter new Department (or press Enter to keep '" + instructor.getDepartment() + "'): ");
            String department = scanner.nextLine();
            if (department.isEmpty()) {
                department = instructor.getDepartment();
            }

            Instructor updatedInstructor = new Instructor(fId, new Name(firstName, lastName), email, department);
            instructorService.updateInstructor(updatedInstructor);
            System.out.println("Instructor details updated successfully.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (DataIntegrityException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }


    private static void addCourse() {
        System.out.println("\n--- Add New Course ---");
        try {
            System.out.print("Enter Course Code (e.g., CS101): ");
            CourseCode code = new CourseCode(scanner.nextLine());
            System.out.print("Enter Course Title: ");
            String title = scanner.nextLine();
            int credits = getUserIntInput("Enter Credits: ");
            System.out.print("Enter Department: ");
            String department = scanner.nextLine();
            System.out.println("Available semesters: " + Arrays.toString(Semester.values()));
            System.out.print("Enter Semester: ");
            String semesterStr = scanner.nextLine().toUpperCase();

            Semester semester = Semester.valueOf(semesterStr);
            Course course = new Course.Builder(code)
                    .withTitle(title)
                    .withCredits(credits)
                    .withDepartment(department)
                    .withSemester(semester)
                    .build();
            courseService.addCourse(course);
            System.out.println("Course added successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid semester provided.");
        } catch (DataIntegrityException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void listAllCourses() {
        System.out.println("\n--- All Courses ---");
        List<Course> courses = courseService.getAllCoursesSortedByCode();
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
        } else {
            printCoursesTable(courses);
        }
    }

    private static void searchAndFilterCourses() {
        System.out.println("\n--- Search & Filter Courses ---");
        System.out.println("1. Filter by Department");
        System.out.println("2. Filter by Semester");
        System.out.println("3. Filter by Instructor FiD");
        int choice = getUserIntInput("Enter choice: ");

        List<Course> filteredCourses = new ArrayList<>();
        Predicate<Course> filter;

        switch (choice) {
            case 1:
                System.out.print("Enter department: ");
                String dept = scanner.nextLine();
                filter = courseService.byDepartment(dept);
                filteredCourses = courseService.filterCourses(filter);
                break;
            case 2:
                System.out.print("Enter semester (" + Arrays.toString(Semester.values()) + "): ");
                String semStr = scanner.nextLine().toUpperCase();
                try {
                    Semester sem = Semester.valueOf(semStr);
                    filter = courseService.bySemester(sem);
                    filteredCourses = courseService.filterCourses(filter);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid semester.");
                    return;
                }
                break;
            case 3:
                System.out.print("Enter instructor FiD: ");
                String instructorId = scanner.nextLine();
                filter = courseService.byInstructor(instructorId);
                filteredCourses = courseService.filterCourses(filter);
                break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        if (filteredCourses.isEmpty()) {
            System.out.println("No courses match the criteria.");
        } else {
            printCoursesTable(filteredCourses);
        }
    }

    private static void assignInstructorToCourse() {
        System.out.println("\n--- Assign Instructor to Course ---");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        System.out.print("Enter Instructor FiD to assign: ");
        String instructorId = scanner.nextLine();
        try {
            courseService.assignInstructor(courseCode, instructorId);
            System.out.println("Instructor assigned successfully.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void enrollStudent() {
        System.out.println("\n--- Enroll Student in Course ---");
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        try {
            enrollmentService.enrollStudent(regNo, courseCode);
            System.out.println("Enrollment successful.");
        } catch (DuplicateEnrollmentException | MaxCreditLimitExceededException | RecordNotFoundException e) {
            System.err.println("Enrollment failed: " + e.getMessage());
        }
    }

    private static void unenrollStudent() {
        System.out.println("\n--- Unenroll Student from Course ---");
        System.out.print("Enter Student Registration Number: ");
        String regNo = scanner.nextLine();
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        try {
            enrollmentService.unenrollStudent(regNo, courseCode);
            System.out.println("Unenrollment successful.");
        } catch (RecordNotFoundException e) {
            System.err.println("Unenrollment failed: " + e.getMessage());
        }
    }

    private static void recordGrade() {
        System.out.println("\n--- Record Student's Grade ---");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());

        try {
            List<Student> enrolledStudents = enrollmentService.getEnrolledStudents(courseCode);
            if (enrolledStudents.isEmpty()) {
                System.out.println("No students are enrolled in this course.");
                return;
            }

            System.out.println("Available grades: " + Arrays.toString(Grade.values()));
            for (Student student : enrolledStudents) {
                System.out.printf("Enter grade for %s: ", student.getRegNo());
                String gradeStr = scanner.nextLine().toUpperCase();
                try {
                    Grade grade = Grade.valueOf(gradeStr);
                    enrollmentService.recordGrade(student.getRegNo(), courseCode, grade);
                    System.out.println("Grade recorded successfully.");
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid grade provided. Skipping this student.");
                } catch (RecordNotFoundException e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void viewAllEnrollmentsByStudent() {
        System.out.println("\n--- All Enrollments by Student ---");
        List<Student> students = studentService.getAllStudentsSortedById();
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        for (Student student : students) {
            System.out.println("\n--- Enrollments for " + student.getFullName() + " (Reg No: " + student.getRegNo() + ") ---");
            try {
                Thread.sleep(2000);
                List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(student.getRegNo());
                if (enrollments.isEmpty()) {
                    System.out.println("No enrollments found for this student.");
                } else {
                    String[] headers = {"Course Code", "Course Title", "Credits", "Grade"};
                    List<String[]> rows = new ArrayList<>();
                    for (Enrollment enrollment : enrollments) {
                        rows.add(new String[]{
                                enrollment.getCourse().getCourseCode().getCode(),
                                enrollment.getCourse().getTitle(),
                                String.valueOf(enrollment.getCourse().getCredits()),
                                enrollment.getGrade() != null ? enrollment.getGrade().toString() : "Not Graded"
                        });
                    }
                    printTable(headers, rows);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Delay interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Could not retrieve enrollments for student " + student.getRegNo() + ": " + e.getMessage());
            }
        }
    }

    private static void exportData() {
        System.out.println("\n--- Export Data to CSV ---");
        try {
            importExportService.exportData();
        } catch (IOException e) {
            System.err.println("Error during export: " + e.getMessage());
        }
    }

    private static void createBackup() {
        System.out.println("\n--- Create a Backup ---");
        try {
            backupService.performBackup();
        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
        }
    }

    private static void showBackupSize() {
        System.out.println("\n--- Show Backup Directory Size ---");
        Path backupDir = AppConfig.getInstance().getBackupDirectory();
        try {
            if (Files.exists(backupDir)) {
                long size = RecursiveUtil.calculateDirectorySize(backupDir);
                System.out.printf("Total size of backups directory '%s' is %,d bytes.%n", backupDir, size);
            } else {
                System.out.println("Backup directory does not exist yet.");
            }
        } catch (IOException e) {
            System.err.println("Could not calculate directory size: " + e.getMessage());
        }
    }

    private static int getUserIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Please enter a number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private static void printJavaPlatformInfo() {
        System.out.println("\n--- About Java Platforms ---");
        System.out.println("Java SE (Standard Edition): The core Java platform for developing desktop, server, and console applications.");
        System.out.println("Java EE (Enterprise Edition): Built on top of Java SE, it provides APIs for large-scale, multi-tiered, and reliable enterprise applications.");
        System.out.println("Java ME (Micro Edition): A subset of Java SE for developing applications for mobile devices and embedded systems with limited resources.");
        System.out.println("----------------------------");
    }

    private static void printTable(String[] headers, List<String[]> rows) {
        int[] maxWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            maxWidths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (row[i].length() > maxWidths[i]) {
                    maxWidths[i] = row[i].length();
                }
            }
        }
        StringBuilder formatBuilder = new StringBuilder();
        for (int maxWidth : maxWidths) {
            formatBuilder.append("| %-").append(maxWidth).append("s ");
        }
        formatBuilder.append("|\n");
        String format = formatBuilder.toString();
        System.out.printf(format, (Object[]) headers);
        for (int maxWidth : maxWidths) {
            for (int i = 0; i < maxWidth + 3; i++) {
                System.out.print("-");
            }
        }
        System.out.println("-");
        for (String[] row : rows) {
            System.out.printf(format, (Object[]) row);
        }
    }

    private static void printCoursesTable(List<Course> courses) {
        String[] headers = {"Code", "Title", "Credits", "Department", "Semester", "Instructor"};
        List<String[]> rows = new ArrayList<>();
        for (Course c : courses) {
            rows.add(new String[]{
                    c.getCourseCode().getCode(),
                    c.getTitle(),
                    String.valueOf(c.getCredits()),
                    c.getDepartment(),
                    c.getSemester().toString(),
                    c.getInstructor() != null ? c.getInstructor().getFullName().toString() : "N/A"
            });
        }
        printTable(headers, rows);
    }
}