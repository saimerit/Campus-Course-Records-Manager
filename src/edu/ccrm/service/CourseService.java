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

    private final InstructorService instructorService = new InstructorService();

    public void addCourse(Course course) {
        try (Connection conn = DatabaseManager.getConnection()) {
            addCourse(course, conn);
        } catch (SQLException e) {
            System.err.println("Error getting database connection: " + e.getMessage());
        }
    }

    public void addCourse(Course course, Connection conn) {
        if (courseExists(course.getCourseCode())) {
            throw new DataIntegrityException("Course with code " + course.getCourseCode() + " already exists.");
        }
        String sql = "INSERT INTO courses (code, title, credits, department, instructor_id, semester) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseCode().getCode());
            pstmt.setString(2, course.getTitle());
            pstmt.setInt(3, course.getCredits());
            pstmt.setString(4, course.getDepartment());
            if (course.getInstructor() != null) {
                pstmt.setInt(5, course.getInstructor().getId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.setString(6, course.getSemester().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Course findCourseByCode(CourseCode courseCode) {
        String sql = "SELECT * FROM courses WHERE code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode.getCode());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int instructorId = rs.getInt("instructor_id");
                Instructor instructor = null;
                if (!rs.wasNull()) {
                    instructor = instructorService.findInstructorById(instructorId);
                }
                return new Course.Builder(new CourseCode(rs.getString("code")))
                        .withTitle(rs.getString("title"))
                        .withCredits(rs.getInt("credits"))
                        .withDepartment(rs.getString("department"))
                        .withSemester(Semester.valueOf(rs.getString("semester")))
                        .withInstructor(instructor)
                        .build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RecordNotFoundException("Course with code " + courseCode + " not found.");
    }

    public void assignInstructor(CourseCode courseCode, int instructorId) {
        findCourseByCode(courseCode);
        instructorService.findInstructorById(instructorId);
        String sql = "UPDATE courses SET instructor_id = ? WHERE code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructorId);
            pstmt.setString(2, courseCode.getCode());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Course> getAllCoursesSortedByCode() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.code, c.title, c.credits, c.department, c.semester, " +
                     "i.id as inst_id, i.first_name, i.last_name, i.email as inst_email, i.department as inst_dept " +
                     "FROM courses c LEFT JOIN instructors i ON c.instructor_id = i.id ORDER BY c.code ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Instructor instructor = null;
                int instructorId = rs.getInt("inst_id");
                if (!rs.wasNull()) {
                    instructor = new Instructor(
                        instructorId,
                        new Name(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("inst_email"),
                        rs.getString("inst_dept")
                    );
                }
                Course course = new Course.Builder(new CourseCode(rs.getString("code")))
                        .withTitle(rs.getString("title"))
                        .withCredits(rs.getInt("credits"))
                        .withDepartment(rs.getString("department"))
                        .withSemester(Semester.valueOf(rs.getString("semester")))
                        .withInstructor(instructor)
                        .build();
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public List<Course> filterCourses(Predicate<Course> predicate) {
        return getAllCoursesSortedByCode().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public Predicate<Course> byInstructor(int instructorId) {
        return course -> course.getInstructor() != null && course.getInstructor().getId() == instructorId;
    }

    public Predicate<Course> byDepartment(String department) {
        return course -> course.getDepartment().equalsIgnoreCase(department);
    }

    public Predicate<Course> bySemester(Semester semester) {
        return course -> course.getSemester() == semester;
    }

    private boolean courseExists(CourseCode courseCode) {
        String sql = "SELECT COUNT(*) FROM courses WHERE code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode.getCode());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}