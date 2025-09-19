package edu.ccrm.io;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.service.CourseService;
import edu.ccrm.service.InstructorService;
import edu.ccrm.service.StudentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ImportExportService {

    private final AppConfig config = AppConfig.getInstance();
    private final StudentService studentService = new StudentService();
    private final InstructorService instructorService = new InstructorService();
    private final CourseService courseService = new CourseService();

    public void importData() {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); 

            System.out.println("Importing data from CSV...");
            importInstructors(Path.of("test-data/instructors.csv"), conn);
            System.out.println("Instructors imported.");
            importStudents(Path.of("test-data/students.csv"), conn);
            System.out.println("Students imported.");
            importCourses(Path.of("test-data/courses.csv"), conn);
            System.out.println("Courses imported.");

            conn.commit(); 
            System.out.println("All data imported successfully and committed.");

        } catch (IOException | SQLException e) {
            System.err.println("Error during import process: " + e.getMessage());
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back.");
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private void importStudents(Path filePath, Connection conn) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Student student = new Student(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        new Name(parts[2], parts[3]),
                        parts[4],
                        Student.Status.valueOf(parts[5]),
                        LocalDate.parse(parts[6])
                );
                try {
                    studentService.addStudent(student, conn);
                } catch (DataIntegrityException e) {
                    System.err.println("Skipping duplicate student: " + e.getMessage());
                }
            }
        }
    }

    private void importInstructors(Path filePath, Connection conn) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Instructor instructor = new Instructor(
                        Integer.parseInt(parts[0]),
                        new Name(parts[1], parts[2]),
                        parts[3],
                        parts[4]
                );
                try {
                    instructorService.addInstructor(instructor, conn);
                } catch (DataIntegrityException e) {
                    System.err.println("Skipping duplicate instructor: " + e.getMessage());
                }
            }
        }
    }

    private void importCourses(Path filePath, Connection conn) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                CourseCode courseCode = new CourseCode(parts[0]);
                try {
                    Instructor instructor = instructorService.findInstructorById(Integer.parseInt(parts[4]));
                    Course course = new Course.Builder(courseCode)
                            .withTitle(parts[1])
                            .withCredits(Integer.parseInt(parts[2]))
                            .withDepartment(parts[3])
                            .withInstructor(instructor)
                            .withSemester(Semester.valueOf(parts[5]))
                            .build();
                    courseService.addCourse(course, conn);
                } catch (RecordNotFoundException e) {
                    System.err.println("Skipping course " + courseCode + " because instructor was not found: " + e.getMessage());
                } catch (DataIntegrityException e) {
                    System.err.println("Skipping duplicate course: " + e.getMessage());
                }
            }
        }
    }
    
    public void exportData() throws IOException {
        Files.createDirectories(config.getDataDirectory());
        System.out.println("Exporting data from database...");
        exportStudents(config.getStudentsFilePath());
        exportInstructors(config.getInstructorsFilePath());
        exportCourses(config.getCoursesFilePath());
        System.out.println("Data exported successfully.");
    }
    
    private void exportStudents(Path path) throws IOException {
        List<String> lines = studentService.getAllStudentsSortedById().stream()
            .map(Student::toCsvString)
            .collect(Collectors.toList());
        lines.add(0, "id,regNo,firstName,lastName,email,status,registrationDate");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private void exportInstructors(Path path) throws IOException {
        List<String> lines = instructorService.getAllInstructorsSortedById().stream()
            .map(Instructor::toCsvString)
            .collect(Collectors.toList());
        lines.add(0, "id,firstName,lastName,email,department");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void exportCourses(Path path) throws IOException {
        List<String> lines = courseService.getAllCoursesSortedByCode().stream()
            .map(c -> String.join(",",
                c.getCourseCode().getCode(),
                c.getTitle(),
                String.valueOf(c.getCredits()),
                c.getDepartment(),
                (c.getInstructor() != null) ? String.valueOf(c.getInstructor().getId()) : "",
                c.getSemester().name()
            ))
            .collect(Collectors.toList());
        lines.add(0, "code,title,credits,department,instructorId,semester");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}