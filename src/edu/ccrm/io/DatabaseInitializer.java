package edu.ccrm.io;

import edu.ccrm.service.CourseService;
import edu.ccrm.service.EnrollmentService;
import edu.ccrm.service.InstructorService;
import edu.ccrm.service.StudentService;
import edu.ccrm.service.ProbationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

public class DatabaseInitializer {

  private static final String SCRIPT_FILE_PATH = "database_setup.sql";
  private static final String[] EXPECTED_TABLES = {
    "INSTRUCTORS",
    "STUDENTS",
    "COURSES",
    "ENROLLMENTS",
    "PROBATION_REPORTS",
    "PROBATION_STUDENTS",
  };

  private static boolean firstRun = false;

  public static boolean isFirstRun() {
    return firstRun;
  }

  public static void initialize() {
    System.out.println("--- Database Initialization ---");
    try (Connection conn = DatabaseManager.getConnection()) {
      System.out.println(
        "? Database connection successful for user '" +
        conn.getMetaData().getUserName() +
        "'."
      );
      System.out.println("    - Verifying schema...");

      if (verifySchema(conn)) {
        System.out.println("? Schema already exists. Skipping setup script.");
        firstRun = false;
      } else {
        System.out.println("    - Schema not found. Running setup script...");
        runScriptFromFile(SCRIPT_FILE_PATH);

        System.out.println("    - Verifying schema again...");
        if (verifySchema(conn)) {
          System.out.println(
            "? Schema verification successful. All tables are present."
          );
          firstRun = true; // Signal to UI that this is a fresh install
        } else {
          throw new RuntimeException(
            "Schema verification failed after running setup script. Check permissions and SQL script."
          );
        }
      }
    } catch (SQLException | IOException e) {
      System.err.println("? CRITICAL FAILURE DURING DATABASE INITIALIZATION:");
      System.err.println("    " + e.getMessage());
      throw new RuntimeException("Could not initialize the database.", e);
    }
  }

  public static void importSampleData() {
      importSampleData(true, true, true, true, true);
  }

  public static void importSampleData(boolean impInstructors, boolean impCourses, boolean impStudents, boolean impEnrollments, boolean impProbation) {
    System.out.println("    - Importing sample data...");
    try {
      StudentService studentService = new StudentService();
      InstructorService instructorService = new InstructorService();
      CourseService courseService = new CourseService(instructorService);
      EnrollmentService enrollmentService = new EnrollmentService(studentService, courseService);
      ProbationService probationService = new ProbationService();
      ImportExportService importExportService = new ImportExportService(
        studentService, instructorService, courseService, enrollmentService, probationService
      );
      if (impStudents) {
          importExportService.importStudents();
      }
      if (impInstructors) {
          importExportService.importInstructors();
          Thread.sleep(1000);
      }
      if (impCourses) {
          importExportService.importCourses();
          Thread.sleep(1000);
      }
      if (impEnrollments) {
          importExportService.importEnrollments();
          Thread.sleep(1000);
      }
      if (impProbation) {
          importExportService.importProbationReports();
      }
      System.out.println("? Sample data imported successfully.");
    } catch (InterruptedException e) {
      System.err.println("Data import process was interrupted.");
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      System.err.println("An unexpected error occurred during sample data import: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void importInitialData() {
    System.out.println("    - Importing initial data...");
    try {
      StudentService studentService = new StudentService();
      InstructorService instructorService = new InstructorService();
      CourseService courseService = new CourseService(instructorService);
      EnrollmentService enrollmentService = new EnrollmentService(
        studentService,
        courseService
      );
      ProbationService probationService = new ProbationService();
      ImportExportService importExportService = new ImportExportService(
        studentService,
        instructorService,
        courseService,
        enrollmentService,
        probationService
      );

      importExportService.importStudents();
      importExportService.importInstructors();

      Thread.sleep(5000);

      importExportService.importCourses();

      Thread.sleep(5000);

      importExportService.importEnrollments();

      System.out.println("? Initial data imported successfully.");
    } catch (InterruptedException e) {
      System.err.println("Data import process was interrupted.");
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      System.err.println(
        "An unexpected error occurred during data import: " + e.getMessage()
      );
      e.printStackTrace();
    }
  }

  private static void runScriptFromFile(String filePath)
    throws IOException, SQLException {
    Path scriptPath = Path.of(filePath);
    String content = Files.readString(scriptPath);

    content = content.replaceAll("--.*", "").trim();
    StringTokenizer tokenizer = new StringTokenizer(content, ";/", false);

    try (
      Connection conn = DatabaseManager.getConnection();
      Statement stmt = conn.createStatement()
    ) {
      while (tokenizer.hasMoreTokens()) {
        String sql = tokenizer.nextToken().trim();
        if (sql.isEmpty()) {
          continue;
        }

        try {
          stmt.execute(sql);
        } catch (SQLException e) {
          if (e.getErrorCode() == 942) {} else {
            System.err.println("SQL Error on statement: " + sql);
            throw e;
          }
        }
      }
    }
  }

  private static boolean verifySchema(Connection conn) throws SQLException {
    String[] coreTables = {"INSTRUCTORS", "STUDENTS", "COURSES", "ENROLLMENTS"};
    for (String tableName : coreTables) {
      try (
        ResultSet rs = conn
          .getMetaData()
          .getTables(null, null, tableName, null)
      ) {
        if (!rs.next()) {
          System.err.println(
            "    - VERIFICATION FAILED: Core table '" + tableName + "' is missing."
          );
          return false;
        }
      }
    }

    // Check mandatory columns and trigger migration for any that are missing
    try (ResultSet rs = conn.getMetaData().getColumns(null, null, "STUDENTS", "DOB")) {
        if (!rs.next()) {
            System.err.println("    - VERIFICATION FAILED: Column 'DOB' missing in 'STUDENTS'. Rebuilding schema.");
            return false;
        }
    }
    try (ResultSet rs = conn.getMetaData().getColumns(null, null, "COURSES", "CLASSROOM_NO")) {
        if (!rs.next()) {
            System.err.println("    - VERIFICATION FAILED: Column 'CLASSROOM_NO' missing in 'COURSES'. Rebuilding schema.");
            return false;
        }
    }

    // Live migrations — add new enrollment columns if not present
    migrateEnrollmentColumns(conn);
    return true;
  }

  private static void migrateEnrollmentColumns(Connection conn) {
    try (Statement stmt = conn.createStatement()) {
        // Add enrollment_year if missing
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "ENROLLMENTS", "ENROLLMENT_YEAR")) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE ENROLLMENTS ADD enrollment_year NUMBER(4)");
                System.out.println("    - Migrated: Added ENROLLMENT_YEAR column to ENROLLMENTS.");
            }
        }
        // Add enrollment_semester if missing
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "ENROLLMENTS", "ENROLLMENT_SEMESTER")) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE ENROLLMENTS ADD enrollment_semester VARCHAR2(20)");
                System.out.println("    - Migrated: Added ENROLLMENT_SEMESTER column to ENROLLMENTS.");
            }
        }
        // Add grand_total_marks if missing
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "ENROLLMENTS", "GRAND_TOTAL_MARKS")) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE ENROLLMENTS ADD grand_total_marks NUMBER(5,2) DEFAULT 0");
                System.out.println("    - Migrated: Added GRAND_TOTAL_MARKS column to ENROLLMENTS.");
            }
        }
        // Add cgpa to STUDENTS if missing
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "STUDENTS", "CGPA")) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE STUDENTS ADD cgpa NUMBER(3,2)");
                System.out.println("    - Migrated: Added CGPA column to STUDENTS.");
            }
        }
        // Add probation_count to STUDENTS if missing
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "STUDENTS", "PROBATION_COUNT")) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE STUDENTS ADD probation_count NUMBER DEFAULT 0");
                System.out.println("    - Migrated: Added PROBATION_COUNT column to STUDENTS.");
            }
        }
        
        // Create DROPPED_ENROLLMENTS table if missing
        boolean droppedTableExists = false;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "DROPPED_ENROLLMENTS", null)) {
            if (rs.next()) {
                droppedTableExists = true;
            }
        }
        if (!droppedTableExists) {
            stmt.executeUpdate("CREATE TABLE DROPPED_ENROLLMENTS (student_reg_no VARCHAR2(20), course_code VARCHAR2(10), drop_date DATE)");
            System.out.println("    - Migrated: Created DROPPED_ENROLLMENTS table.");
        }
        
        // Create PROBATION_REPORTS table if missing (or recreate if old schema found)
        boolean probationReportsExists = false;
        boolean hasProbationIdColumn = false;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "PROBATION_REPORTS", null)) {
            if (rs.next()) {
                probationReportsExists = true;
            }
        }
        if (probationReportsExists) {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "PROBATION_REPORTS", "PROBATION_ID")) {
                if (rs.next()) {
                    hasProbationIdColumn = true;
                }
            }
        }
        if (probationReportsExists && !hasProbationIdColumn) {
            System.out.println("    - Detected old PROBATION_REPORTS schema. Recreating...");
            stmt.executeUpdate("DROP TABLE PROBATION_REPORTS CASCADE CONSTRAINTS");
            probationReportsExists = false;
        }

        if (!probationReportsExists) {
            stmt.executeUpdate("CREATE TABLE PROBATION_REPORTS ("
                + "probation_id VARCHAR2(20) PRIMARY KEY, "
                + "start_date DATE NOT NULL, "
                + "end_date DATE NOT NULL, "
                + "reason VARCHAR2(1000) NOT NULL"
                + ")");
            System.out.println("    - Migrated: Created PROBATION_REPORTS table.");
        }

        // Create PROBATION_STUDENTS table if missing
        boolean probationStudentsExists = false;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "PROBATION_STUDENTS", null)) {
            if (rs.next()) {
                probationStudentsExists = true;
            }
        }
        if (!probationStudentsExists) {
            stmt.executeUpdate("CREATE TABLE PROBATION_STUDENTS ("
                + "probation_id VARCHAR2(20) NOT NULL, "
                + "student_reg_no VARCHAR2(20) NOT NULL, "
                + "PRIMARY KEY (probation_id, student_reg_no), "
                + "CONSTRAINT fk_probation_report FOREIGN KEY (probation_id) REFERENCES PROBATION_REPORTS(probation_id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_probation_student_reg FOREIGN KEY (student_reg_no) REFERENCES STUDENTS(reg_no)"
                + ")");
            System.out.println("    - Migrated: Created PROBATION_STUDENTS table.");
        }
    } catch (SQLException e) {
        System.err.println("Warning: Schema migration encountered an issue: " + e.getMessage());
    }
  }
}