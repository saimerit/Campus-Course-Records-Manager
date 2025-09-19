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
import java.util.ArrayList;
import java.util.List;

public class EnrollmentService {

    private static final int MAX_CREDITS = 21;
    private final StudentService studentService;
    private final CourseService courseService;

    public EnrollmentService(StudentService studentService, CourseService courseService) {
        this.studentService = studentService;
        this.courseService = courseService;
    }

    public void enrollStudent(int studentId, CourseCode courseCode)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            enrollStudent(studentId, courseCode, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during enrollment: " + e.getMessage(), e);
        }
    }

    public void enrollStudent(int studentId, CourseCode courseCode, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        if (isEnrolled(studentId, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int currentCredits = getCurrentCredits(studentId, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);

        if (currentCredits + course.getCredits() > MAX_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum credit limit of " + MAX_CREDITS);
        }

        String sql = "INSERT INTO enrollments (student_id, course_code) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, courseCode.getCode());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RecordNotFoundException("Failed to enroll student: " + e.getMessage(), e);
        }
    }

    public void unenrollStudent(int studentId, CourseCode courseCode) throws RecordNotFoundException {
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment record not found for student " + studentId + " in course " + courseCode);
            }
        } catch (SQLException e) {
             throw new RecordNotFoundException("Error during unenrollment: " + e.getMessage(), e);
        }
    }

    public void recordGrade(int studentId, CourseCode courseCode, Grade grade) throws RecordNotFoundException {
        String sql = "UPDATE enrollments SET grade = ? WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grade.name());
            pstmt.setInt(2, studentId);
            pstmt.setString(3, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment record not found for student " + studentId + " in course " + courseCode);
            }
        } catch (SQLException e) {
            throw new RecordNotFoundException("Error recording grade: " + e.getMessage(), e);
        }
    }
    
    public List<Student> getEnrolledStudents(CourseCode courseCode) throws RecordNotFoundException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.* FROM students s JOIN enrollments e ON s.id = e.student_id WHERE e.course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode.getCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(this.studentService.mapRowToStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new RecordNotFoundException("Error fetching enrolled students: " + e.getMessage(), e);
        }
        return students;
    }

    public List<Enrollment> getAllEnrollments() {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT student_id, course_code, grade FROM enrollments";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Student student = this.studentService.findStudentById(rs.getInt("student_id"));
                    Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                    
                    Enrollment enrollment = new Enrollment(student, course);
                    String gradeStr = rs.getString("grade");
                    if (gradeStr != null) {
                        enrollment.setGrade(Grade.valueOf(gradeStr));
                    }
                    enrollments.add(enrollment);

                } catch (RecordNotFoundException e) {
                     System.err.println("Skipping enrollment record due to missing data: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all enrollments: " + e.getMessage());
        }
        return enrollments;
    }
    
    public List<Enrollment> getEnrollmentsForStudent(int studentId) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT student_id, course_code, grade FROM enrollments WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                     try {
                        Student student = this.studentService.findStudentById(rs.getInt("student_id"));
                        Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                        
                        Enrollment enrollment = new Enrollment(student, course);
                        String gradeStr = rs.getString("grade");
                        if (gradeStr != null) {
                            enrollment.setGrade(Grade.valueOf(gradeStr));
                        }
                        enrollments.add(enrollment);

                    } catch (RecordNotFoundException e) {
                         System.err.println("Skipping enrollment record due to missing data: " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error fetching enrollments for student: " + e.getMessage());
        }
        return enrollments;
    }


    private boolean isEnrolled(int studentId, CourseCode courseCode, Connection conn) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, courseCode.getCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
             System.err.println("Error checking enrollment status: " + e.getMessage());
        }
        return false;
    }

    private int getCurrentCredits(int studentId, Connection conn) {
        int totalCredits = 0;
        String sql = "SELECT SUM(c.credits) FROM courses c JOIN enrollments e ON c.code = e.course_code WHERE e.student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalCredits = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating current credits: " + e.getMessage());
        }
        return totalCredits;
    }
}