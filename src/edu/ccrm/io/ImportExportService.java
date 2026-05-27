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
import edu.ccrm.service.ProbationService;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ImportExportService {

  private final AppConfig config = AppConfig.getInstance();
  private final StudentService studentService;
  private final InstructorService instructorService;
  private final CourseService courseService;
  private final EnrollmentService enrollmentService;
  private final ProbationService probationService;

  public ImportExportService(
    StudentService studentService,
    InstructorService instructorService,
    CourseService courseService,
    EnrollmentService enrollmentService,
    ProbationService probationService
  ) {
    this.studentService = studentService;
    this.instructorService = instructorService;
    this.courseService = courseService;
    this.enrollmentService = enrollmentService;
    this.probationService = probationService;
  }

  // Progress callback for record-level reporting
  public interface ImportProgressCallback {
      void onProgress(int processed, int total);
  }

  private long countDataLines(Path filePath) throws IOException {
      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
          long count = 0;
          reader.readLine(); // skip header
          while (reader.readLine() != null) count++;
          return count;
      }
  }

  // =========================================================================
  // STUDENTS IMPORT (BATCH & TRANSACTIONAL)
  // =========================================================================
  public void importStudents() {
    System.out.println("     - Importing students.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importStudents(Path.of("import-data/students.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported students.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during student import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  public void importStudentsFromTestData() {
    System.out.println("     - Importing students.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importStudents(Path.of("test-data/students.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported students.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during student import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  private void importStudents(Path filePath, Connection conn) throws IOException, SQLException {
      importStudents(filePath, conn, null, new int[]{0}, 0);
  }

  private void importStudents(Path filePath, Connection conn, ImportProgressCallback callback, int[] processed, int total)
    throws IOException, SQLException {
    String sql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status, registration_date, dob, phone, probation_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);
         BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine(); // skip header
      int count = 0;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length < 7) continue;
        String status = parts[5].replaceAll("\"", "");
        pstmt.setInt(1, Integer.parseInt(parts[0]));
        pstmt.setString(2, parts[1]);
        pstmt.setString(3, parts[2]);
        pstmt.setString(4, parts[3]);
        pstmt.setString(5, parts[4]);
        pstmt.setString(6, Student.Status.valueOf(status).name());
        pstmt.setDate(7, java.sql.Date.valueOf(parseDateRobust(parts[6])));
        pstmt.setDate(8, parts.length > 7 ? (parseDateRobust(parts[7]) != null ? java.sql.Date.valueOf(parseDateRobust(parts[7])) : null) : null);
        pstmt.setString(9, parts.length > 8 ? parts[8] : null);
        pstmt.setInt(10, parts.length > 9 ? Integer.parseInt(parts[9].replaceAll("\"", "").trim()) : 0);

        pstmt.addBatch();
        count++;
        processed[0]++;
        if (count % 1000 == 0) {
          pstmt.executeBatch();
        }
        if (callback != null) callback.onProgress(processed[0], total);
      }
      if (count % 1000 != 0) {
        pstmt.executeBatch();
      }
    }
  }

  public void importStudentsFile(Path path, ImportProgressCallback callback) throws Exception {
    int total = (int) countDataLines(path);
    int[] processed = {0};
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importStudents(path, conn, callback, processed, total);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void importStudentsFile(Path path) throws Exception {
      importStudentsFile(path, null);
  }

  // =========================================================================
  // INSTRUCTORS IMPORT (BATCH & TRANSACTIONAL)
  // =========================================================================
  public void importInstructors() {
    System.out.println("     - Importing instructors.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importInstructors(Path.of("import-data/instructors.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported instructors.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during instructor import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  public void importInstructorsFromTestData() {
    System.out.println("     - Importing instructors.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importInstructors(Path.of("test-data/instructors.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported instructors.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during instructor import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  private void importInstructors(Path filePath, Connection conn) throws IOException, SQLException {
      importInstructors(filePath, conn, null, new int[]{0}, 0);
  }

  private void importInstructors(Path filePath, Connection conn, ImportProgressCallback callback, int[] processed, int total)
    throws IOException, SQLException {
    String sql = "INSERT INTO instructors (FiD, first_name, last_name, email, department, dob, phone, cabin_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);
         BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine(); // Skip header
      int count = 0;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length < 5) continue;
        pstmt.setString(1, parts[0]);
        pstmt.setString(2, parts[1]);
        pstmt.setString(3, parts[2]);
        pstmt.setString(4, parts[3]);
        pstmt.setString(5, parts[4]);
        pstmt.setDate(6, parts.length > 5 ? (parseDateRobust(parts[5]) != null ? java.sql.Date.valueOf(parseDateRobust(parts[5])) : null) : null);
        pstmt.setString(7, parts.length > 6 ? parts[6] : null);
        pstmt.setString(8, parts.length > 7 ? parts[7] : null);

        pstmt.addBatch();
        count++;
        processed[0]++;
        if (count % 1000 == 0) {
          pstmt.executeBatch();
        }
        if (callback != null) callback.onProgress(processed[0], total);
      }
      if (count % 1000 != 0) {
        pstmt.executeBatch();
      }
    }
  }

  public void importInstructorsFile(Path path, ImportProgressCallback callback) throws Exception {
    int total = (int) countDataLines(path);
    int[] processed = {0};
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importInstructors(path, conn, callback, processed, total);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void importInstructorsFile(Path path) throws Exception {
      importInstructorsFile(path, null);
  }

  // =========================================================================
  // COURSES IMPORT (BATCH & TRANSACTIONAL)
  // =========================================================================
  public void importCourses() {
    System.out.println("     - Importing courses.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importCourses(Path.of("import-data/courses.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported courses.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during course import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  public void importCoursesFromTestData() {
    System.out.println("     - Importing courses.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importCourses(Path.of("test-data/courses.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported courses.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during course import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  private void importCourses(Path filePath, Connection conn) throws IOException, SQLException {
      importCourses(filePath, conn, null, new int[]{0}, 0);
  }

  private void importCourses(Path filePath, Connection conn, ImportProgressCallback callback, int[] processed, int total)
    throws IOException, SQLException {
    String sql = "INSERT INTO courses (code, title, credits, department, instructor_id, semester, classroom_no) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);
         BufferedReader reader = Files.newBufferedReader(filePath)) {
      String line;
      reader.readLine(); // skip header
      int count = 0;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length < 6) continue;
        CourseCode courseCode = new CourseCode(parts[0]);

        // Instructor validation & lookup
        Instructor instructor = null;
        try {
          instructor = instructorService.findInstructorByFiD(parts[4], conn);
        } catch (RecordNotFoundException e) {
          System.err.println("Warning: Skipping course " + courseCode + " because instructor was not found: " + e.getMessage());
          processed[0]++;
          if (callback != null) callback.onProgress(processed[0], total);
          continue;
        }

        pstmt.setString(1, courseCode.getCode());
        pstmt.setString(2, parts[1]);
        pstmt.setInt(3, Integer.parseInt(parts[2]));
        pstmt.setString(4, parts[3]);
        if (instructor != null) {
          pstmt.setString(5, instructor.getFiD());
        } else {
          pstmt.setNull(5, Types.VARCHAR);
        }
        pstmt.setString(6, Semester.valueOf(parts[5]).name());
        pstmt.setString(7, parts.length > 6 ? parts[6] : null);

        pstmt.addBatch();
        count++;
        processed[0]++;
        if (count % 1000 == 0) {
          pstmt.executeBatch();
        }
        if (callback != null) callback.onProgress(processed[0], total);
      }
      if (count % 1000 != 0) {
        pstmt.executeBatch();
      }
    }
  }

  public void importCoursesFile(Path path, ImportProgressCallback callback) throws Exception {
    int total = (int) countDataLines(path);
    int[] processed = {0};
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importCourses(path, conn, callback, processed, total);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void importCoursesFile(Path path) throws Exception {
      importCoursesFile(path, null);
  }

  // =========================================================================
  // ENROLLMENTS IMPORT (ORACLE BATCHED MERGE & TRANSACTIONAL)
  // =========================================================================
  public void importEnrollments() {
    System.out.println("     - Importing enrollments.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);
        try {
            importEnrollments(Path.of("import-data/enrollments.csv"), conn);
            conn.commit();
            System.out.println("✔ Successfully imported enrollments.csv");
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

  public void importEnrollmentsFromTestData() {
    System.out.println("     - Importing enrollments.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);
        try {
            importEnrollments(Path.of("test-data/enrollments.csv"), conn);
            conn.commit();
            System.out.println("✔ Successfully imported enrollments.csv");
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
      importEnrollments(filePath, conn, null, new int[]{0}, 0);
  }

  private void importEnrollments(Path filePath, Connection conn, ImportProgressCallback callback, int[] processed, int total) throws IOException, SQLException {
      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
          String headerLine = reader.readLine(); // Read header to find column positions
          if (headerLine == null) return;
          String[] headers = headerLine.split(",");
          if (headers.length > 0 && headers[0].startsWith("\ufeff")) {
              headers[0] = headers[0].substring(1);
          }
          int idxStudent = -1, idxCourse = -1, idxGrade = -1, idxYear = -1, idxSemester = -1;
          for (int i = 0; i < headers.length; i++) {
              switch (headers[i].trim().toLowerCase()) {
                  case "studentregno": idxStudent = i; break;
                  case "coursecode":   idxCourse = i; break;
                  case "grade":        idxGrade = i; break;
                  case "enrollmentyear": idxYear = i; break;
                  case "semester":     idxSemester = i; break;
              }
          }
          if (idxStudent < 0 || idxCourse < 0) return;

          // Highly resilient Oracle upsert MERGE query
          String mergeSql = "MERGE INTO enrollments e "
                          + "USING (SELECT ? AS student_reg_no, ? AS course_code, ? AS enrollment_year, ? AS enrollment_semester, ? AS grade FROM dual) src "
                          + "ON (e.student_reg_no = src.student_reg_no AND e.course_code = src.course_code) "
                          + "WHEN MATCHED THEN "
                          + "  UPDATE SET e.grade = src.grade "
                          + "WHEN NOT MATCHED THEN "
                          + "  INSERT (student_reg_no, course_code, enrollment_year, enrollment_semester, grade) "
                          + "  VALUES (src.student_reg_no, src.course_code, src.enrollment_year, src.enrollment_semester, src.grade)";

          try (PreparedStatement pstmt = conn.prepareStatement(mergeSql)) {
              String line;
              int count = 0;
              while ((line = reader.readLine()) != null) {
                  String[] parts = line.split(",");
                  if (parts.length < 2) continue;

                  String studentRegNo = parts[idxStudent].trim();
                  CourseCode courseCode = new CourseCode(parts[idxCourse].trim());
                  int enrollYear = (idxYear >= 0 && idxYear < parts.length && !parts[idxYear].trim().isEmpty())
                                   ? Integer.parseInt(parts[idxYear].trim())
                                   : java.time.LocalDate.now().getYear();
                  String csvSemester = (idxSemester >= 0 && idxSemester < parts.length && !parts[idxSemester].trim().isEmpty())
                                   ? parts[idxSemester].trim().toUpperCase()
                                   : "";

                  // Retrieve Course to find Semester or auto-create it
                  String semester = "";
                  try {
                      Course course = courseService.findCourseByCode(courseCode, conn);
                      semester = course.getSemester() != null ? course.getSemester().name() : "";
                  } catch (RecordNotFoundException e) {
                      semester = !csvSemester.isEmpty() ? csvSemester : "FALL";
                      try {
                          String insertCourseSql = "INSERT INTO courses (code, title, credits, department, semester) VALUES (?, ?, ?, ?, ?)";
                          try (PreparedStatement coursePstmt = conn.prepareStatement(insertCourseSql)) {
                              coursePstmt.setString(1, courseCode.getCode());
                              coursePstmt.setString(2, courseCode.getCode() + " Placeholder");
                              coursePstmt.setInt(3, 3);
                              coursePstmt.setString(4, "General");
                              coursePstmt.setString(5, semester);
                              coursePstmt.executeUpdate();
                          }
                          System.out.println("    - Auto-created placeholder course: " + courseCode.getCode());
                      } catch (SQLException sqle) {
                          System.err.println("Warning: Failed to auto-create course " + courseCode.getCode() + ": " + sqle.getMessage());
                          processed[0]++;
                          if (callback != null) callback.onProgress(processed[0], total);
                          continue;
                      }
                  }

                  // Retrieve Student to verify existence or auto-create it
                  try {
                      studentService.findStudentByRegNo(studentRegNo, conn);
                  } catch (RecordNotFoundException e) {
                      try {
                          String insertStudentSql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
                          try (PreparedStatement studentPstmt = conn.prepareStatement(insertStudentSql)) {
                              int placeholderId = 900000 + Math.abs(studentRegNo.hashCode() % 100000);
                              studentPstmt.setInt(1, placeholderId);
                              studentPstmt.setString(2, studentRegNo);
                              studentPstmt.setString(3, "Placeholder");
                              studentPstmt.setString(4, studentRegNo);
                              studentPstmt.setString(5, studentRegNo.toLowerCase() + "@placeholder.com");
                              studentPstmt.setString(6, "ACTIVE");
                              studentPstmt.setDate(7, java.sql.Date.valueOf(java.time.LocalDate.now()));
                              studentPstmt.executeUpdate();
                          }
                          System.out.println("    - Auto-created placeholder student: " + studentRegNo);
                      } catch (SQLException sqle) {
                          System.err.println("Warning: Failed to auto-create student " + studentRegNo + ": " + sqle.getMessage());
                          processed[0]++;
                          if (callback != null) callback.onProgress(processed[0], total);
                          continue;
                      }
                  }

                  // Retrieve business credit limits (warn only)
                  int currentTotalCredits = getCurrentCreditsDirect(studentRegNo, conn);
                  int currentSemesterCredits = getCurrentSemesterYearCreditsDirect(studentRegNo, semester, enrollYear, conn);

                  try {
                      Course course = courseService.findCourseByCode(courseCode, conn);
                      if (currentTotalCredits + course.getCredits() > 225) {
                          System.err.println("Warning: Credit limit validation: student " + studentRegNo + " enrollment in " + courseCode.getCode() + " would exceed total credits (225). Proceeding anyway.");
                      }
                      if (currentSemesterCredits + course.getCredits() > 60) {
                          System.err.println("Warning: Credit limit validation: student " + studentRegNo + " enrollment in " + courseCode.getCode() + " would exceed semester credits (60). Proceeding anyway.");
                      }
                  } catch (RecordNotFoundException e) {
                      // Already handled or created placeholder
                  }

                  // Setup parameters
                  pstmt.setString(1, studentRegNo);
                  pstmt.setString(2, courseCode.getCode());
                  pstmt.setInt(3, enrollYear);
                  pstmt.setString(4, semester);
                  if (idxGrade >= 0 && idxGrade < parts.length && !parts[idxGrade].trim().isEmpty()) {
                      pstmt.setString(5, parts[idxGrade].trim().toUpperCase());
                  } else {
                      pstmt.setNull(5, Types.VARCHAR);
                  }

                  pstmt.addBatch();
                  count++;
                  processed[0]++;
                  if (count % 1000 == 0) {
                      pstmt.executeBatch();
                  }
                  if (callback != null) callback.onProgress(processed[0], total);
              }
              if (count % 1000 != 0) {
                  pstmt.executeBatch();
              }
          }
      }
  }

  public void importEnrollmentsFile(Path path, ImportProgressCallback callback) throws Exception {
    int total = (int) countDataLines(path);
    int[] processed = {0};
    try (Connection conn = DatabaseManager.getConnection()) {
        conn.setAutoCommit(false);
        try {
            importEnrollments(path, conn, callback, processed, total);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
  }

  public void importEnrollmentsFile(Path path) throws Exception {
      importEnrollmentsFile(path, null);
  }

  // =========================================================================
  // PROBATION REPORTS IMPORT (BATCH & TRANSACTIONAL)
  // =========================================================================
  private List<String[]> parseCsv(Path path) throws IOException {
      List<String[]> rows = new ArrayList<>();
      try (BufferedReader br = Files.newBufferedReader(path)) {
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
      return rows;
  }

  public void importProbationReports() {
    System.out.println("     - Importing probation_reports.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importProbationReports(Path.of("import-data/probation_reports.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported probation_reports.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during probation report import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  public void importProbationReportsFromTestData() {
    System.out.println("     - Importing probation_reports.csv...");
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importProbationReports(Path.of("test-data/probation_reports.csv"), conn);
        conn.commit();
        System.out.println("✔ Successfully imported probation_reports.csv");
      } catch (IOException | SQLException | RuntimeException e) {
        conn.rollback();
        System.err.println("Error during probation report import: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.err.println("Database connection error: " + e.getMessage());
    }
  }

  private void importProbationReports(Path filePath, Connection conn) throws IOException, SQLException {
      importProbationReports(filePath, conn, null, new int[]{0}, 0);
  }

  private void importProbationReports(Path filePath, Connection conn, ImportProgressCallback callback, int[] processed, int total)
    throws IOException, SQLException {
    List<String[]> rows = parseCsv(filePath);
    if (rows.isEmpty()) return;
    
    String insertReportSql = "INSERT INTO probation_reports (probation_id, start_date, end_date, reason) VALUES (?, ?, ?, ?)";
    String insertStudentSql = "INSERT INTO probation_students (probation_id, student_reg_no) VALUES (?, ?)";
    String updateStudentStatusSql = "UPDATE students SET status = 'PROBATION', probation_count = probation_count + 1 WHERE reg_no = ?";
    
    try (PreparedStatement pstmtReport = conn.prepareStatement(insertReportSql);
         PreparedStatement pstmtStudent = conn.prepareStatement(insertStudentSql);
         PreparedStatement pstmtStatus = conn.prepareStatement(updateStudentStatusSql)) {
      
      int count = 0;
      for (int i = 1; i < rows.size(); i++) {
        String[] parts = rows.get(i);
        if (parts.length < 5) continue;
        
        String probationId = parts[0];
        String regNosStr = parts[1].replaceAll("\"", "");
        LocalDate start = parseDateRobust(parts[2]);
        LocalDate end = parseDateRobust(parts[3]);
        String reason = parts[4].replaceAll("\"", "");
        
        List<String> regNos = java.util.Arrays.stream(regNosStr.split(";"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        pstmtReport.setString(1, probationId);
        pstmtReport.setDate(2, java.sql.Date.valueOf(start));
        pstmtReport.setDate(3, java.sql.Date.valueOf(end));
        pstmtReport.setString(4, reason);
        pstmtReport.executeUpdate();
        
        for (String regNo : regNos) {
            try {
                studentService.findStudentByRegNo(regNo, conn);
                
                pstmtStudent.setString(1, probationId);
                pstmtStudent.setString(2, regNo);
                pstmtStudent.executeUpdate();
                
                pstmtStatus.setString(1, regNo);
                pstmtStatus.executeUpdate();
            } catch (RecordNotFoundException e) {
                System.err.println("Warning: Skipping probation student association for " + regNo + " - Student not found.");
            }
        }
        
        count++;
        processed[0]++;
        if (callback != null) callback.onProgress(processed[0], total);
      }
    }
  }

  public void importProbationReportsFile(Path path, ImportProgressCallback callback) throws Exception {
    int total = (int) countDataLines(path);
    int[] processed = {0};
    try (Connection conn = DatabaseManager.getConnection()) {
      conn.setAutoCommit(false);
      try {
        importProbationReports(path, conn, callback, processed, total);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void importProbationReportsFile(Path path) throws Exception {
      importProbationReportsFile(path, null);
  }

  // =========================================================================
  // CREDIT DIRECT CALCULATORS FOR ENROLLMENTS
  // =========================================================================
  private int getCurrentCreditsDirect(String studentRegNo, Connection conn) throws SQLException {
      int totalCredits = 0;
      String sql = "SELECT SUM(c.credits) FROM courses c JOIN enrollments e ON c.code = e.course_code WHERE e.student_reg_no = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          pstmt.setString(1, studentRegNo);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  totalCredits = rs.getInt(1);
              }
          }
      }
      return totalCredits;
  }

  private int getCurrentSemesterYearCreditsDirect(String studentRegNo, String semester, int year, Connection conn) throws SQLException {
      int semesterCredits = 0;
      String sql = "SELECT SUM(c.credits) FROM courses c JOIN enrollments e ON c.code = e.course_code "
                 + "WHERE e.student_reg_no = ? AND e.enrollment_semester = ? AND e.enrollment_year = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
          pstmt.setString(1, studentRegNo);
          pstmt.setString(2, semester);
          pstmt.setInt(3, year);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  semesterCredits = rs.getInt(1);
              }
          }
      }
      return semesterCredits;
  }

  // =========================================================================
  // EXPORT UTILITIES
  // =========================================================================
  public void exportData() throws IOException {
    Files.createDirectories(config.getDataDirectory());
    System.out.println("Exporting data from database...");
    exportStudents(config.getStudentsFilePath());
    exportInstructors(config.getInstructorsFilePath());
    exportCourses(config.getCoursesFilePath());
    exportEnrollments(config.getEnrollmentsFilePath());
    exportProbationReports(config.getDataDirectory().resolve("probation_reports.csv"));
    System.out.println("Data exported successfully.");
  }

  private void exportStudents(Path path) throws IOException {
    List<String> lines = studentService
      .getAllStudentsSortedById()
      .stream()
      .map(Student::toCsvString)
      .collect(Collectors.toList());
    lines.add(0, "id,regNo,firstName,lastName,email,status,registrationDate,dob,phone,probationCount");
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
    lines.add(0, "FiD,firstName,lastName,email,department,dob,phone,cabinNo");
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
          c.getSemester().name(),
          c.getClassroomNo() != null ? c.getClassroomNo() : ""
        )
      )
      .collect(Collectors.toList());
    lines.add(0, "code,title,credits,department,instructorId,semester,classroomNo");
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

  private void exportProbationReports(Path path) throws IOException {
    List<String> lines = probationService
      .getAllProbationReports()
      .stream()
      .map(r ->
        String.join(
          ",",
          r.getProbationId(),
          "\"" + String.join(";", r.getStudentRegNos()) + "\"",
          r.getStartDate().toString(),
          r.getEndDate().toString(),
          "\"" + r.getReason().replace("\"", "\"\"") + "\""
        )
      )
      .collect(Collectors.toList());
    lines.add(0, "probationId,studentRegNos,startDate,endDate,reason");
    Files.write(
      path,
      lines,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }

  private LocalDate parseDateRobust(String dateStr) {
      if (dateStr == null || dateStr.trim().isEmpty()) return null;
      dateStr = dateStr.trim();
      try {
          return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
      } catch (Exception e1) {
          try {
              return LocalDate.parse(dateStr);
          } catch (Exception e2) {
              try {
                  return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
              } catch (Exception e3) {
                  return null;
              }
          }
      }
  }
}