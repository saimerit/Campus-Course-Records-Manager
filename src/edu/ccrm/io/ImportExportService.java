package edu.ccrm.io;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.DuplicateEnrollmentException;
import edu.ccrm.exception.MaxCreditLimitExceededException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.service.CourseService;
import edu.ccrm.service.EnrollmentService;
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
  private final StudentService studentService;
  private final InstructorService instructorService;
  private final CourseService courseService;
  private final EnrollmentService enrollmentService;

  public ImportExportService(
    StudentService studentService,
    InstructorService instructorService,
    CourseService courseService,
    EnrollmentService enrollmentService
  ) {
    this.studentService = studentService;
    this.instructorService = instructorService;
    this.courseService = courseService;
    this.enrollmentService = enrollmentService;
  }

  public void importStudents() {
    System.out.println("     - Importing students.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importStudents(Path.of("import-data/students.csv"), conn);
      System.out.println("? Successfully imported students.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during student import: " + e.getMessage());
    }
  }

  private void importStudents(Path filePath, Connection conn)
    throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        String status = parts[5].replaceAll("\"", "");
        Student student = new Student(
          Integer.parseInt(parts[0]),
          parts[1],
          new Name(parts[2], parts[3]),
          parts[4],
          Student.Status.valueOf(status),
          LocalDate.parse(parts[6])
        );
        try {
          studentService.addStudent(student, conn);
        } catch (DataIntegrityException e) {
          System.err.println(
            "Student with registration number " +
            student.getRegNo() +
            " already exists."
          );
        }
      }
    }
  }

  public void importInstructors() {
    System.out.println("     - Importing instructors.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importInstructors(Path.of("import-data/instructors.csv"), conn);
      System.out.println("? Successfully imported instructors.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during instructor import: " + e.getMessage());
    }
  }

  private void importInstructors(Path filePath, Connection conn)
    throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine(); // Skip header
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        Instructor instructor = new Instructor(
          parts[0],
          new Name(parts[1], parts[2]),
          parts[3],
          parts[4]
        );
        try {
          instructorService.addInstructor(instructor, conn);
        } catch (DataIntegrityException e) {
          System.err.println(
            "Instructor with FiD " + instructor.getFiD() + " already exists."
          );
        }
      }
    }
  }

  public void importCourses() {
    System.out.println("     - Importing courses.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importCourses(Path.of("import-data/courses.csv"), conn);
      System.out.println("? Successfully imported courses.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during course import: " + e.getMessage());
    }
  }

  private void importCourses(Path filePath, Connection conn)
    throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        CourseCode courseCode = new CourseCode(parts[0]);
        try {
          Instructor instructor = instructorService.findInstructorByFiD(
            parts[4],
            conn
          );
          Course course = new Course.Builder(courseCode)
            .withTitle(parts[1])
            .withCredits(Integer.parseInt(parts[2]))
            .withDepartment(parts[3])
            .withInstructor(instructor)
            .withSemester(Semester.valueOf(parts[5]))
            .build();
          courseService.addCourse(course, conn);
        } catch (RecordNotFoundException e) {
          System.err.println(
            "Skipping course " +
            courseCode +
            " because instructor was not found: " +
            e.getMessage()
          );
        } catch (DataIntegrityException e) {
          System.err.println(
            "Course with code " + courseCode.getCode() + " already exists."
          );
        }
      }
    }
  }

  public void importEnrollments() {
    System.out.println("     - Importing enrollments.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);
        try {
            importEnrollments(Path.of("import-data/enrollments.csv"), conn);
            conn.commit();
            System.out.println("? Successfully imported enrollments.csv");
        } catch (IOException | SQLException | RuntimeException e) {
            conn.rollback();
            System.err.println("An unexpected error occurred during data import: " + e.getMessage());
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true);
        }
    } catch (SQLException e) {
        System.err.println("Database connection error during enrollment import: " + e.getMessage());
    }
  }

  private void importEnrollments(Path filePath, Connection conn) throws IOException, SQLException {
      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
          String line;
          reader.readLine(); // Skip header
          while ((line = reader.readLine()) != null) {
              String[] parts = line.split(",");
              if (parts.length < 2) continue;

              String studentRegNo = parts[0].trim();
              CourseCode courseCode = new CourseCode(parts[1].trim());

              try {
                  if (parts.length > 2 && parts[2] != null && !parts[2].trim().isEmpty()) {
                      Grade grade = Grade.valueOf(parts[2].trim().toUpperCase());
                      enrollmentService.enrollStudentWithGrade(studentRegNo, courseCode, grade, conn);
                  } else {
                      enrollmentService.enrollStudent(studentRegNo, courseCode, conn);
                  }
              } catch (DuplicateEnrollmentException e) {
                  System.out.println("Info: Student " + studentRegNo + " is already enrolled in " + courseCode.getCode() + ". Attempting to update grade.");
                  if (parts.length > 2 && parts[2] != null && !parts[2].trim().isEmpty()) {
                      try {
                         Grade grade = Grade.valueOf(parts[2].trim().toUpperCase());
                         enrollmentService.recordGrade(studentRegNo, courseCode, grade);
                      }
                      catch (Exception gradeException){
                          System.err.println("Could not record grade for already enrolled student.");
                      }
                  }
              } catch (MaxCreditLimitExceededException | RecordNotFoundException | IllegalArgumentException e) {
                  System.err.println("Warning: Could not process enrollment for " + studentRegNo + " in " + courseCode.getCode() + ". Reason: " + e.getMessage());
              }
          }
      }
  }

  public void importStudentsFromTestData() {
    System.out.println("     - Importing students.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importStudents(Path.of("test-data/students.csv"), conn);
      System.out.println("? Successfully imported students.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during student import: " + e.getMessage());
    }
  }


  public void importInstructorsFromTestData() {
    System.out.println("     - Importing instructors.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importInstructors(Path.of("test-data/instructors.csv"), conn);
      System.out.println("? Successfully imported instructors.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during instructor import: " + e.getMessage());
    }
  }

  public void importCoursesFromTestData() {
    System.out.println("     - Importing courses.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      importCourses(Path.of("test-data/courses.csv"), conn);
      System.out.println("? Successfully imported courses.csv");
    } catch (IOException | SQLException e) {
      System.err.println("Error during course import: " + e.getMessage());
    }
  }

  public void importEnrollmentsFromTestData() {
    System.out.println("     - Importing enrollments.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);
        try {
            importEnrollments(Path.of("test-data/enrollments.csv"), conn);
            conn.commit();
            System.out.println("? Successfully imported enrollments.csv");
        } catch (IOException | SQLException | RuntimeException e) {
            conn.rollback();
            System.err.println("An unexpected error occurred during data import: " + e.getMessage());
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true);
        }
    } catch (SQLException e) {
        System.err.println("Database connection error during enrollment import: " + e.getMessage());
    }
  }


  public void exportData() throws IOException {
    Files.createDirectories(config.getDataDirectory());
    System.out.println("Exporting data from database...");
    exportStudents(config.getStudentsFilePath());
    exportInstructors(config.getInstructorsFilePath());
    exportCourses(config.getCoursesFilePath());
    exportEnrollments(config.getEnrollmentsFilePath());
    System.out.println("Data exported successfully.");
  }

  private void exportStudents(Path path) throws IOException {
    List<String> lines = studentService
      .getAllStudentsSortedById()
      .stream()
      .map(Student::toCsvString)
      .collect(Collectors.toList());
    lines.add(0, "id,regNo,firstName,lastName,email,status,registrationDate");
    Files.write(
      path,
      lines,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }

  private void exportInstructors(Path path) throws IOException {
    List<String> lines = instructorService
      .getAllInstructorsSortedById()
      .stream()
      .map(Instructor::toCsvString)
      .collect(Collectors.toList());
    lines.add(0, "FiD,firstName,lastName,email,department");
    Files.write(
      path,
      lines,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }

  private void exportCourses(Path path) throws IOException {
    List<String> lines = courseService
      .getAllCoursesSortedByCode()
      .stream()
      .map(c ->
        String.join(
          ",",
          c.getCourseCode().getCode(),
          c.getTitle(),
          String.valueOf(c.getCredits()),
          c.getDepartment(),
          (c.getInstructor() != null)
            ? c.getInstructor().getFiD()
            : "",
          c.getSemester().name()
        )
      )
      .collect(Collectors.toList());
    lines.add(0, "code,title,credits,department,instructorId,semester");
    Files.write(
      path,
      lines,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }

  private void exportEnrollments(Path path) throws IOException {
    List<String> lines = enrollmentService
      .getAllEnrollments()
      .stream()
      .map(e ->
        String.join(
          ",",
          e.getStudent().getRegNo(),
          e.getCourse().getCourseCode().getCode(),
          e.getGrade() != null ? e.getGrade().name() : ""
        )
      )
      .collect(Collectors.toList());
    lines.add(0, "studentRegNo,courseCode,grade");
    Files.write(
      path,
      lines,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }
}