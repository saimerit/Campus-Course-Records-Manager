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

    private static final int MAX_TOTAL_CREDITS = 225;
    private static final int MAX_SEMESTER_CREDITS = 60;
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

    public void enrollStudent(String studentRegNo, CourseCode courseCode, int enrollmentYear)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            enrollStudent(studentRegNo, courseCode, enrollmentYear, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during enrollment: " + e.getMessage(), e);
        }
    }

    public void enrollStudent(String studentRegNo, CourseCode courseCode, int enrollmentYear, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException, SQLException {
        if (isEnrolled(studentRegNo, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int currentTotalCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);
        String semester = course.getSemester() != null ? course.getSemester().name() : "";
        int currentSemesterCredits = getCurrentSemesterYearCredits(studentRegNo, semester, enrollmentYear, conn);

        if (currentTotalCredits + course.getCredits() > MAX_TOTAL_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum total credit limit of " + MAX_TOTAL_CREDITS);
        }

        if (currentSemesterCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the per-semester credit limit of " + MAX_SEMESTER_CREDITS
                    + " for " + semester + " " + enrollmentYear);
        }

        String sql = "INSERT INTO enrollments (student_reg_no, course_code, enrollment_year, enrollment_semester) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.setInt(3, enrollmentYear);
            pstmt.setString(4, semester);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Failed to enroll student: " + e.getMessage(), e);
        }
    }

    public void enrollStudent(String studentRegNo, CourseCode courseCode, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException, SQLException {
        if (isEnrolled(studentRegNo, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int enrollYear = java.time.LocalDate.now().getYear();
        int currentTotalCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);
        String semester = course.getSemester() != null ? course.getSemester().name() : "";
        int currentSemesterCredits = getCurrentSemesterYearCredits(studentRegNo, semester, enrollYear, conn);

        if (currentTotalCredits + course.getCredits() > MAX_TOTAL_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum total credit limit of " + MAX_TOTAL_CREDITS);
        }

        if (currentSemesterCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the per-semester credit limit of " + MAX_SEMESTER_CREDITS
                    + " for " + semester + " " + enrollYear);
        }

        String sql = "INSERT INTO enrollments (student_reg_no, course_code, enrollment_year, enrollment_semester) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.setInt(3, enrollYear);
            pstmt.setString(4, semester);
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

        int enrollYear = java.time.LocalDate.now().getYear();
        int currentTotalCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);
        String semester = course.getSemester() != null ? course.getSemester().name() : "";
        int currentSemesterCredits = getCurrentSemesterYearCredits(studentRegNo, semester, enrollYear, conn);

        if (currentTotalCredits + course.getCredits() > MAX_TOTAL_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum total credit limit of " + MAX_TOTAL_CREDITS);
        }

        if (currentSemesterCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the per-semester credit limit of " + MAX_SEMESTER_CREDITS
                    + " for " + semester + " " + enrollYear);
        }

        String insertSql = "INSERT INTO enrollments (student_reg_no, course_code, enrollment_year, enrollment_semester) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.setInt(3, enrollYear);
            pstmt.setString(4, semester);
            pstmt.executeUpdate();
        }
        recordGrade(studentRegNo, courseCode, grade, conn);
    }


    public void unenrollStudent(String studentRegNo, CourseCode courseCode) throws RecordNotFoundException {
        String insertSql = "INSERT INTO DROPPED_ENROLLMENTS (student_reg_no, course_code, drop_date) VALUES (?, ?, ?)";
        String deleteSql = "DELETE FROM enrollments WHERE student_reg_no = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setString(1, studentRegNo);
                    pstmt.setString(2, courseCode.getCode());
                    pstmt.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
                    pstmt.executeUpdate();
                }
                int affectedRows;
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setString(1, studentRegNo);
                    pstmt.setString(2, courseCode.getCode());
                    affectedRows = pstmt.executeUpdate();
                }
                if (affectedRows == 0) {
                    throw new RecordNotFoundException("Enrollment record not found for student " + studentRegNo + " in course " + courseCode);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
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

    public void recordGrade(String studentRegNo, CourseCode courseCode, Grade grade, Connection conn) throws RecordNotFoundException, SQLException {
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
        String sql = "SELECT student_reg_no, course_code, grade, enrollment_year, enrollment_semester FROM enrollments";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Student student = this.studentService.findStudentByRegNo(rs.getString("student_reg_no"), conn);
                    Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                    String gradeStr = rs.getString("grade");
                    Grade grade = (gradeStr != null && !gradeStr.isEmpty()) ? Grade.valueOf(gradeStr) : Grade.NA;
                    int year = rs.getInt("enrollment_year");
                    String sem = rs.getString("enrollment_semester");
                    Enrollment enrollment = new Enrollment(student, course, grade, year, sem);
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
        String sql = "SELECT e.student_reg_no, e.course_code, e.grade, e.enrollment_year, e.enrollment_semester FROM enrollments e INNER JOIN courses c ON e.course_code = c.code WHERE e.student_reg_no = ? ORDER BY e.enrollment_year ASC, e.enrollment_semester ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentRegNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        Student student = this.studentService.findStudentByRegNo(rs.getString("student_reg_no"), conn);
                        Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                        String gradeStr = rs.getString("grade");
                        Grade grade = (gradeStr != null && !gradeStr.isEmpty()) ? Grade.valueOf(gradeStr) : Grade.NA;
                        int year = rs.getInt("enrollment_year");
                        String sem = rs.getString("enrollment_semester");
                        Enrollment enrollment = new Enrollment(student, course, grade, year, sem);
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

    public List<Enrollment> getEnrollmentsForCourse(String courseCode) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT e.student_reg_no, e.course_code, e.grade, e.enrollment_year, e.enrollment_semester FROM enrollments e INNER JOIN courses c ON e.course_code = c.code WHERE e.course_code = ? ORDER BY e.enrollment_year ASC, e.enrollment_semester ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        Student student = this.studentService.findStudentByRegNo(rs.getString("student_reg_no"), conn);
                        Course course = this.courseService.findCourseByCode(new CourseCode(rs.getString("course_code")), conn);
                        String gradeStr = rs.getString("grade");
                        Grade grade = (gradeStr != null && !gradeStr.isEmpty()) ? Grade.valueOf(gradeStr) : Grade.NA;
                        int year = rs.getInt("enrollment_year");
                        String sem = rs.getString("enrollment_semester");
                        Enrollment enrollment = new Enrollment(student, course, grade, year, sem);
                        enrollments.add(enrollment);

                    } catch (RecordNotFoundException | SQLException e) {
                        System.err.println("Skipping enrollment record due to missing data: " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error fetching enrollments for course: " + e.getMessage());
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

    private int getCurrentSemesterYearCredits(String studentRegNo, String semester, int year, Connection conn) throws SQLException {
        int semesterCredits = 0;
        // Query uses enrollment_semester and enrollment_year stored IN the enrollment row
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

    public void enrollStudentWithGradeAndYear(String studentRegNo, CourseCode courseCode, Grade grade, int enrollmentYear, Connection conn)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException, RecordNotFoundException, SQLException {
        if (isEnrolled(studentRegNo, courseCode, conn)) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }
        int currentTotalCredits = getCurrentCredits(studentRegNo, conn);
        Course course = this.courseService.findCourseByCode(courseCode, conn);
        String semester = course.getSemester() != null ? course.getSemester().name() : "";
        int currentSemesterCredits = getCurrentSemesterYearCredits(studentRegNo, semester, enrollmentYear, conn);

        if (currentTotalCredits + course.getCredits() > MAX_TOTAL_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the maximum total credit limit of " + MAX_TOTAL_CREDITS);
        }
        if (currentSemesterCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Enrolling in this course would exceed the per-semester credit limit of " + MAX_SEMESTER_CREDITS
                    + " for " + semester + " " + enrollmentYear);
        }
        String insertSql = "INSERT INTO enrollments (student_reg_no, course_code, enrollment_year, enrollment_semester) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            pstmt.setInt(3, enrollmentYear);
            pstmt.setString(4, semester);
            pstmt.executeUpdate();
        }
        recordGrade(studentRegNo, courseCode, grade, conn);
    }

    public void updateEnrollmentYear(String studentRegNo, CourseCode courseCode, int newYear) throws RecordNotFoundException, MaxCreditLimitExceededException {
        try (Connection conn = DatabaseManager.getConnection()) {
            updateEnrollmentYear(studentRegNo, courseCode, newYear, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating enrollment year: " + e.getMessage(), e);
        }
    }

    public void updateEnrollmentYear(String studentRegNo, CourseCode courseCode, int newYear, Connection conn) throws RecordNotFoundException, MaxCreditLimitExceededException, SQLException {
        int oldYear = -1;
        String checkSql = "SELECT enrollment_year FROM enrollments WHERE student_reg_no = ? AND course_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, studentRegNo);
            pstmt.setString(2, courseCode.getCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    oldYear = rs.getInt(1);
                } else {
                    throw new RecordNotFoundException("Enrollment record not found for student " + studentRegNo + " in course " + courseCode);
                }
            }
        }

        if (oldYear == newYear) {
            return;
        }

        Course course = this.courseService.findCourseByCode(courseCode, conn);
        String semester = course.getSemester() != null ? course.getSemester().name() : "";
        int currentSemesterCredits = getCurrentSemesterYearCredits(studentRegNo, semester, newYear, conn);

        if (currentSemesterCredits + course.getCredits() > MAX_SEMESTER_CREDITS) {
            throw new MaxCreditLimitExceededException(
                    "Updating enrollment year would exceed the per-semester credit limit of " + MAX_SEMESTER_CREDITS
                    + " for " + semester + " " + newYear);
        }

        String updateSql = "UPDATE enrollments SET enrollment_year = ? WHERE student_reg_no = ? AND course_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, newYear);
            pstmt.setString(2, studentRegNo);
            pstmt.setString(3, courseCode.getCode());
            pstmt.executeUpdate();
        }
    }

    /**
     * Data structure to hold raw total marks for calculation
     */
    private static class StudentMarks {
        String studentRegNo;
        double marks;

        StudentMarks(String studentRegNo, double marks) {
            this.studentRegNo = studentRegNo;
            this.marks = marks;
        }
    }

    /**
     * Updates individual student marks for a specific course enrollment instance.
     */
    public void updateStudentMarks(String studentRegNo, CourseCode courseCode, int year, String semester, double marks) 
            throws RecordNotFoundException {
        String selectSql = "SELECT grade, grand_total_marks FROM enrollments " +
                           "WHERE student_reg_no = ? AND course_code = ? AND enrollment_year = ? AND enrollment_semester = ?";
        String updateSql = "UPDATE enrollments SET grand_total_marks = ? " +
                           "WHERE student_reg_no = ? AND course_code = ? AND enrollment_year = ? AND enrollment_semester = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String existingGrade = null;
                double existingMarks = 0.0;
                boolean recordFound = false;

                try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                    selectPstmt.setString(1, studentRegNo);
                    selectPstmt.setString(2, courseCode.getCode());
                    selectPstmt.setInt(3, year);
                    selectPstmt.setString(4, semester);
                    try (ResultSet rs = selectPstmt.executeQuery()) {
                        if (rs.next()) {
                            existingGrade = rs.getString("grade");
                            existingMarks = rs.getDouble("grand_total_marks");
                            recordFound = true;
                        }
                    }
                }

                if (!recordFound) {
                    throw new RecordNotFoundException("Enrollment record not found to update marks for student: " + studentRegNo);
                }

                if (existingGrade != null && !existingGrade.trim().isEmpty() && !existingGrade.equals("NA")) {
                    if (existingMarks == 0.0) {
                        throw new IllegalStateException("Marked data cannot be overwritten without proper change in marks");
                    }
                }

                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setDouble(1, marks);
                    updatePstmt.setString(2, studentRegNo);
                    updatePstmt.setString(3, courseCode.getCode());
                    updatePstmt.setInt(4, year);
                    updatePstmt.setString(5, semester);
                    updatePstmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating student marks: " + e.getMessage(), e);
        }
    }

    /**
     * Computes relative grading strictly for a targeted cohort: Course + Year + Semester
     */
    public void calculateRelativeGrading(CourseCode courseCode, int year, String semester) 
            throws RecordNotFoundException {
        
        String selectSql = "SELECT student_reg_no, grand_total_marks FROM enrollments " +
                           "WHERE course_code = ? AND enrollment_year = ? AND enrollment_semester = ?";
        
        List<StudentMarks> cohortList = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            
            pstmt.setString(1, courseCode.getCode());
            pstmt.setInt(2, year);
            pstmt.setString(3, semester);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cohortList.add(new StudentMarks(
                        rs.getString("student_reg_no"),
                        rs.getDouble("grand_total_marks")
                    ));
                }
            }
            
            if (cohortList.isEmpty()) {
                throw new RecordNotFoundException("No student enrollments found for the specified class batch.");
            }
            
            // 1. Calculate Mean
            double sum = 0;
            for (StudentMarks sm : cohortList) {
                sum += sm.marks;
            }
            double mean = sum / cohortList.size();
            
            // 2. Calculate Sample Standard Deviation
            double stdev = 0;
            if (cohortList.size() > 1) {
                double varianceSum = 0;
                for (StudentMarks sm : cohortList) {
                    varianceSum += Math.pow(sm.marks - mean, 2);
                }
                stdev = Math.sqrt(varianceSum / (cohortList.size() - 1));
            }
            
            // 3. Evaluate grades and persist them in a single batch transaction
            String updateGradeSql = "UPDATE enrollments SET grade = ? " +
                                    "WHERE student_reg_no = ? AND course_code = ? AND enrollment_year = ? AND enrollment_semester = ?";
            
            conn.setAutoCommit(false);
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateGradeSql)) {
                for (StudentMarks sm : cohortList) {
                    String assignedGrade = determineRelativeGrade(sm.marks, mean, stdev);
                    
                    updatePstmt.setString(1, assignedGrade);
                    updatePstmt.setString(2, sm.studentRegNo);
                    updatePstmt.setString(3, courseCode.getCode());
                    updatePstmt.setInt(4, year);
                    updatePstmt.setString(5, semester);
                    updatePstmt.addBatch();
                }
                updatePstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new SQLException("Failed batch operation during relative grade distribution calculation", e);
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Database execution fault during relative grading calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Maps performance strictly to your Python mathematical logic model boundaries
     */
    private String determineRelativeGrade(double marks, double mean, double stdev) {
        // Absolute failure catch
        if (marks < 40) {
            return Grade.F.name(); // Assuming your Grade Enum has standard patterns
        }
        
        if (marks >= mean + (1.5 * stdev)) {
            return "S";
        } else if (marks >= mean + stdev) {
            return "A";
        } else if (marks >= mean + (0.5 * stdev)) {
            return "B";
        } else if (marks >= mean) {
            return "C";
        } else if (marks >= mean - (0.5 * stdev)) {
            return "D";
        } else if (marks >= mean - stdev) {
            return "E";
        } else {
            return "F";
        }
    }
}