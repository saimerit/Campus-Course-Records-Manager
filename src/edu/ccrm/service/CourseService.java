package edu.ccrm.service;

import edu.ccrm.domain.*;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CourseService {

  private final InstructorService instructorService;

  public CourseService() {
    this.instructorService = new InstructorService();
  }

  public CourseService(InstructorService instructorService) {
    this.instructorService = instructorService;
  }

  public void addCourse(Course course) throws DataIntegrityException {
    try (Connection conn = DatabaseManager.getConnection()) {
      addCourse(course, conn);
    } catch (SQLException e) {
      throw new DataIntegrityException(
        "Database error adding course: " + e.getMessage(),
        e
      );
    }
  }

  public void addCourse(Course course, Connection conn)
    throws DataIntegrityException {
    String sql =
      "INSERT INTO courses (code, title, credits, department, instructor_id, semester) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, course.getCourseCode().getCode());
      pstmt.setString(2, course.getTitle());
      pstmt.setInt(3, course.getCredits());
      pstmt.setString(4, course.getDepartment());
      if (course.getInstructor() != null) {
        pstmt.setString(5, course.getInstructor().getFiD());
      } else {
        pstmt.setNull(5, Types.VARCHAR);
      }
      pstmt.setString(6, course.getSemester().name());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        throw new DataIntegrityException(
          "Course with code " + course.getCourseCode().getCode() +
          " already exists.",
          e
        );
      }
      throw new DataIntegrityException(
        "Error adding course: " + e.getMessage(),
        e
      );
    }
  }

  public List<Course> getAllCoursesSortedByCode() {
    List<Course> courses = new ArrayList<>();
    String sql = "SELECT * FROM courses ORDER BY code";
    try (
      Connection conn = DatabaseManager.getConnection();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(sql)
    ) {
      while (rs.next()) {
        courses.add(mapRowToCourse(rs, conn));
      }
    } catch (SQLException e) {
      System.err.println(
        "Database error fetching all courses: " + e.getMessage()
      );
    }
    return courses;
  }

  public Course findCourseByCode(CourseCode courseCode)
    throws RecordNotFoundException {
    try (Connection conn = DatabaseManager.getConnection()) {
      return findCourseByCode(courseCode, conn);
    } catch (SQLException e) {
      throw new RecordNotFoundException(
        "Database error finding course by code: " + e.getMessage()
      );
    }
  }

  public Course findCourseByCode(CourseCode courseCode, Connection conn)
    throws RecordNotFoundException {
    String sql = "SELECT * FROM courses WHERE code = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, courseCode.getCode());
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapRowToCourse(rs, conn);
        } else {
          throw new RecordNotFoundException(
            "Course with code " + courseCode.getCode() + " not found."
          );
        }
      }
    } catch (SQLException e) {
      throw new RecordNotFoundException(
        "Database error finding course by code: " + e.getMessage()
      );
    }
  }

  public void assignInstructor(CourseCode courseCode, String instructorId)
    throws RecordNotFoundException {
    String sql = "UPDATE courses SET instructor_id = ? WHERE code = ?";
    try (
      Connection conn = DatabaseManager.getConnection();
      PreparedStatement pstmt = conn.prepareStatement(sql)
    ) {
      pstmt.setString(1, instructorId);
      pstmt.setString(2, courseCode.getCode());
      int affectedRows = pstmt.executeUpdate();
      if (affectedRows == 0) {
        throw new RecordNotFoundException(
          "Course with code " + courseCode.getCode() + " not found."
        );
      }
    } catch (SQLException e) {
      throw new RecordNotFoundException(
        "Error assigning instructor: " + e.getMessage(),
        e
      );
    }
  }

  public List<Course> filterCourses(Predicate<Course> predicate) {
    return getAllCoursesSortedByCode()
      .stream()
      .filter(predicate)
      .collect(Collectors.toList());
  }

  public Predicate<Course> byDepartment(String department) {
    return course -> course.getDepartment().equalsIgnoreCase(department);
  }

  public Predicate<Course> bySemester(Semester semester) {
    return course -> course.getSemester() == semester;
  }

  public Predicate<Course> byInstructor(String instructorId) {
    return course ->
      course.getInstructor() != null &&
      course.getInstructor().getFiD().equals(instructorId);
  }

  private Course mapRowToCourse(ResultSet rs, Connection conn)
    throws SQLException {
    CourseCode code = new CourseCode(rs.getString("code"));
    String title = rs.getString("title");
    int credits = rs.getInt("credits");
    String department = rs.getString("department");
    Semester semester = Semester.valueOf(rs.getString("semester"));
    String instructorId = rs.getString("instructor_id");

    Course.Builder builder = new Course.Builder(code)
      .withTitle(title)
      .withCredits(credits)
      .withDepartment(department)
      .withSemester(semester);

    if (instructorId != null && !rs.wasNull()) {
      try {
        Instructor instructor = this.instructorService.findInstructorByFiD(
            instructorId,
            conn
          );
        builder.withInstructor(instructor);
      } catch (RecordNotFoundException e) {
        System.err.println("Warning: " + e.getMessage());
      }
    }
    return builder.build();
  }
}