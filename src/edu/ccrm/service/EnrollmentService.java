package edu.ccrm.service;

import edu.ccrm.domain.*;
import edu.ccrm.exception.DuplicateEnrollmentException;
import edu.ccrm.exception.MaxCreditLimitExceededException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EnrollmentService {
    private final StudentService studentService = new StudentService();
    private final CourseService courseService = new CourseService();

    public void enrollStudent(int studentId, CourseCode courseCode)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        
        studentService.findStudentById(studentId);
        courseService.findCourseByCode(courseCode);

        String checkSql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, studentId);
            ps.setString(2, courseCode.getCode());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String enrollSql = "INSERT INTO enrollments (student_id, course_code, grade) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(enrollSql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, courseCode.getCode());
            pstmt.setString(3, Grade.NA.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unenrollStudent(int studentId, CourseCode courseCode) {
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment record not found for this student and course.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void recordGrade(int studentId, CourseCode courseCode, Grade grade) {
        String sql = "UPDATE enrollments SET grade = ? WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grade.name());
            pstmt.setInt(2, studentId);
            pstmt.setString(3, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                 throw new RecordNotFoundException("Student is not enrolled in this course.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}