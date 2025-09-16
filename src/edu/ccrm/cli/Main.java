package edu.ccrm.cli;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.*;
import edu.ccrm.io.BackupService;
import edu.ccrm.io.ImportExportService;
import edu.ccrm.service.*;
import edu.ccrm.util.RecursiveUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentService studentService = new StudentService();
    private static final CourseService courseService = new CourseService();
    private static final EnrollmentService enrollmentService = new EnrollmentService();
    private static final TranscriptService transcriptService = new TranscriptService();
    private static final ImportExportService importExportService = new ImportExportService();
    private static final BackupService backupService = new BackupService();

    public static void main(String[] args) {
        System.out.println("Welcome to Campus Course & Records Manager (CCRM)");
        printJavaPlatformInfo();
        
        mainMenuLoop:
        while (true) {
            printMainMenu();
            int choice = getUserIntInput("Enter your choice: ");
            switch (choice) {
                case 1: manageStudents(); break;
                case 2: manageCourses(); break;
                case 3: manageEnrollments(); break;
                case 4: manageFileOperations(); break;
                case 5: System.out.println("Exiting application."); break mainMenuLoop;
                default: System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Manage Students");
        System.out.println("2. Manage Courses");
        System.out.println("3. Manage Enrollments & Grades");
        System.out.println("4. File Operations");
        System.out.println("5. Exit");
    }

    private static void manageStudents() {
        while (true) {
            System.out.println("\n--- Student Management ---");
            System.out.println("1. Add New Student");
            System.out.println("2. List All Students");
            System.out.println("3. View Student Profile & Transcript");
            System.out.println("4. Update Student Status");
            System.out.println("5. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1: addStudent(); break;
                case 2: listAllStudents(); break;
                case 3: viewStudentTranscript(); break;
                case 4: updateStudentStatus(); break;
                case 5: return;
                default: System.out.println("Invalid choice.");
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
                case 1: addCourse(); break;
                case 2: listAllCourses(); break;
                case 3: searchAndFilterCourses(); break;
                case 4: assignInstructorToCourse(); break;
                case 5: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private static void manageEnrollments() {
        while (true) {
            System.out.println("\n--- Enrollment & Grading ---");
            System.out.println("1. Enroll Student in Course");
            System.out.println("2. Unenroll Student from Course");
            System.out.println("3. Record Student's Grade");
            System.out.println("4. Back to Main Menu");
            int choice = getUserIntInput("Enter choice: ");
            switch (choice) {
                case 1: enrollStudent(); break;
                case 2: unenrollStudent(); break;
                case 3: recordGrade(); break;
                case 4: return;
                default: System.out.println("Invalid choice.");
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
                case 1: importData(); break;
                case 2: exportData(); break;
                case 3: createBackup(); break;
                case 4: showBackupSize(); break;
                case 5: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private static void addStudent() {
        System.out.println("\n--- Add New Student ---");
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
    }

    private static void listAllStudents() {
        System.out.println("\n--- All Students ---");
        List<Student> students = studentService.getAllStudentsSortedById();
        if (students.isEmpty()) {
            System.out.println("No students found.");
        } else {
            students.forEach(s -> System.out.println(s.getProfile()));
        }
    }

    private static void viewStudentTranscript() {
        System.out.println("\n--- View Student Transcript ---");
        int studentId = getUserIntInput("Enter Student ID: ");
        try {
            String transcript = transcriptService.generateTranscript(studentId);
            System.out.println(transcript);
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void updateStudentStatus() {
        System.out.println("\n--- Update Student Status ---");
        int studentId = getUserIntInput("Enter Student ID: ");
        System.out.println("Available statuses: " + Arrays.toString(Student.Status.values()));
        System.out.print("Enter new status: ");
        String statusStr = scanner.nextLine().toUpperCase();
        try {
            Student.Status newStatus = Student.Status.valueOf(statusStr);
            studentService.updateStudentStatus(studentId, newStatus);
            System.out.println("Status updated successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status provided.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void addCourse() {
        System.out.println("\n--- Add New Course ---");
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
        try {
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
        }
    }

    private static void listAllCourses() {
        System.out.println("\n--- All Courses ---");
        List<Course> courses = courseService.getAllCoursesSortedByCode();
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
        } else {
            courses.forEach(System.out::println);
        }
    }
    
    private static void searchAndFilterCourses() {
        System.out.println("\n--- Search & Filter Courses ---");
        System.out.println("1. Filter by Department");
        System.out.println("2. Filter by Semester");
        System.out.println("3. Filter by Instructor ID");
        int choice = getUserIntInput("Enter choice: ");

        Predicate<Course> filter = course -> true;
        switch (choice) {
            case 1:
                System.out.print("Enter department: ");
                String dept = scanner.nextLine();
                filter = courseService.byDepartment(dept);
                break;
            case 2:
                System.out.print("Enter semester (" + Arrays.toString(Semester.values()) + "): ");
                String semStr = scanner.nextLine().toUpperCase();
                try {
                    Semester sem = Semester.valueOf(semStr);
                    filter = courseService.bySemester(sem);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid semester.");
                    return;
                }
                break;
            case 3:
                int instructorId = getUserIntInput("Enter instructor ID: ");
                filter = courseService.byInstructor(instructorId);
                break;
            default:
                System.out.println("Invalid choice.");
                return;
        }

        List<Course> filteredCourses = courseService.filterCourses(filter);
        if (filteredCourses.isEmpty()) {
            System.out.println("No courses match the criteria.");
        } else {
            filteredCourses.forEach(System.out::println);
        }
    }
    
    private static void assignInstructorToCourse() {
        System.out.println("\n--- Assign Instructor to Course ---");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        int instructorId = getUserIntInput("Enter Instructor ID to assign: ");
        try {
            courseService.assignInstructor(courseCode, instructorId);
            System.out.println("Instructor assigned successfully.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
    
    private static void enrollStudent() {
        System.out.println("\n--- Enroll Student in Course ---");
        int studentId = getUserIntInput("Enter Student ID: ");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        try {
            enrollmentService.enrollStudent(studentId, courseCode);
            System.out.println("Enrollment successful.");
        } catch (DuplicateEnrollmentException | MaxCreditLimitExceededException | RecordNotFoundException e) {
            System.err.println("Enrollment failed: " + e.getMessage());
        }
    }
    
    private static void unenrollStudent() {
        System.out.println("\n--- Unenroll Student from Course ---");
        int studentId = getUserIntInput("Enter Student ID: ");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        try {
            enrollmentService.unenrollStudent(studentId, courseCode);
            System.out.println("Unenrollment successful.");
        } catch (RecordNotFoundException e) {
            System.err.println("Unenrollment failed: " + e.getMessage());
        }
    }
    
    private static void recordGrade() {
        System.out.println("\n--- Record Student's Grade ---");
        int studentId = getUserIntInput("Enter Student ID: ");
        System.out.print("Enter Course Code: ");
        CourseCode courseCode = new CourseCode(scanner.nextLine());
        System.out.println("Available grades: " + Arrays.toString(Grade.values()));
        System.out.print("Enter grade: ");
        String gradeStr = scanner.nextLine().toUpperCase();
        try {
            Grade grade = Grade.valueOf(gradeStr);
            enrollmentService.recordGrade(studentId, courseCode, grade);
            System.out.println("Grade recorded successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid grade provided.");
        } catch (RecordNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
    
    private static void importData() {
        System.out.println("\n--- Import Data from CSV ---");
        System.out.println("This will import instructors, students, and courses from the 'test-data' directory.");
        try {
            importExportService.importInstructors(Path.of("test-data/instructors.csv"));
            System.out.println("Instructors imported.");
            importExportService.importStudents(Path.of("test-data/students.csv"));
            System.out.println("Students imported.");
            importExportService.importCourses(Path.of("test-data/courses.csv"));
            System.out.println("Courses imported.");
            System.out.println("All data imported successfully.");
        } catch (IOException e) {
            System.err.println("Error during import: " + e.getMessage());
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
}
