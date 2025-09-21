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

    public void enrollStudent(String studentRegNo, CourseCode courseCode)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            enrollStudent(studentRegNo, courseCode, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during enrollment: " + e.getMessage(), e);
        }
    }

    public void enrollStudent(String studentRegNo, CourseCode courseCode, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException, SQLException {
        if (isEnrolled(studentRegNo, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int currentCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);

        if (currentCredits + course.getCredits() > MAX_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum credit limit of " + MAX_CREDITS);
        }

        String sql = "INSERT INTO enrollments (student_reg_no, course_code) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Failed to enroll student: " + e.getMessage(), e);
        }
    }

    public void enrollStudentWithGrade(String studentRegNo, CourseCode courseCode, Grade grade)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                enrollStudentWithGrade(studentRegNo, courseCode, grade, conn);
                conn.commit();
            } catch (SQLException | MaxCreditLimitExceededException | RecordNotFoundException | DuplicateEnrollmentException e) {
                conn.rollback();
                // Re-throw the specific, checked exceptions
                if (e instanceof DuplicateEnrollmentException) throw (DuplicateEnrollmentException) e;
                if (e instanceof MaxCreditLimitExceededException) throw (MaxCreditLimitExceededException) e;
                if (e instanceof RecordNotFoundException) throw (RecordNotFoundException) e;
                // Wrap SQLException in a RuntimeException for single-transaction operations
                throw new RuntimeException("Database error during enrollment with grade: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection error during enrollment with grade: " + e.getMessage(), e);
        }
    }

    public void enrollStudentWithGrade(String studentRegNo, CourseCode courseCode, Grade grade, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException, SQLException {
        if (isEnrolled(studentRegNo, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int currentCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);

        if (currentCredits + course.getCredits() > MAX_CREDITS) {
            throw new MaxCreditLimitExceededException("Enrolling in this course would exceed the maximum credit limit.");
        }

        String insertSql = "INSERT INTO enrollments (student_reg_no, course_code) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.executeUpdate();
        }
        recordGrade(studentRegNo, courseCode, grade, conn);
    }


    public void unenrollStudent(String studentRegNo, CourseCode courseCode) throws RecordNotFoundException {
        String sql = "DELETE FROM enrollments WHERE student_reg_no = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment record not found for student " + studentRegNo + " in course " + courseCode);
            }
        } catch (SQLException e) {
             throw new RecordNotFoundException("Error during unenrollment: " + e.getMessage(), e);
        }
    }

    public void recordGrade(String studentRegNo, CourseCode courseCode, Grade grade) throws RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            recordGrade(studentRegNo, courseCode, grade, conn);
        } catch (SQLException e) {
            throw new RecordNotFoundException("Database error recording grade: " + e.getMessage(), e);
        }
    }

    private void recordGrade(String studentRegNo, CourseCode courseCode, Grade grade, Connection conn) throws RecordNotFoundException, SQLException {
        String sql = "UPDATE enrollments SET grade = ? WHERE student_reg_no = ? AND course_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grade.name());
            pstmt.setString(2, studentRegNo);
            pstmt.setString(3, courseCode.getCode());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment record not found for student " + studentRegNo + " in course " + courseCode);
            }
        }
    }
    
    public List<Student> getEnrolledStudents(CourseCode courseCode) throws RecordNotFoundException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.* FROM students s JOIN enrollments e ON s.reg_no = e.student_reg_no WHERE e.course_code = ?";
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
        String sql = "SELECT student_reg_no, course_code, grade FROM enrollments";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Student student = this.studentService.findStudentByRegNo(rs.getString("student_reg_no"));
                    Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                    
                    Enrollment enrollment = new Enrollment(student, course);
                    String gradeStr = rs.getString("grade");
                    if (gradeStr != null) {
                        enrollment.setGrade(Grade.valueOf(gradeStr));
                    }
                    enrollments.add(enrollment);

                } catch (RecordNotFoundException | SQLException e) {
                     System.err.println("Skipping enrollment record due to missing data: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all enrollments: " + e.getMessage());
        }
        return enrollments;
    }
    
    public List<Enrollment> getEnrollmentsForStudent(String studentRegNo) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT student_reg_no, course_code, grade FROM enrollments WHERE student_reg_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentRegNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                     try {
                        Student student = this.studentService.findStudentByRegNo(rs.getString("student_reg_no"));
                        Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                        
                        Enrollment enrollment = new Enrollment(student, course);
                        String gradeStr = rs.getString("grade");
                        if (gradeStr != null) {
                            enrollment.setGrade(Grade.valueOf(gradeStr));
                        }
                        enrollments.add(enrollment);

                    } catch (RecordNotFoundException | SQLException e) {
                         System.err.println("Skipping enrollment record due to missing data: " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error fetching enrollments for student: " + e.getMessage());
        }
        return enrollments;
    }


    private boolean isEnrolled(String studentRegNo, CourseCode courseCode, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_reg_no = ? AND course_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private int getCurrentCredits(String studentRegNo, Connection conn) throws SQLException {
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
}