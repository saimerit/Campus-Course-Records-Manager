package edu.ccrm.service;

import edu.ccrm.domain.*;
import edu.ccrm.io.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TranscriptService {
    private final StudentService studentService = new StudentService();
    private final CourseService courseService = new CourseService();

    public String generateTranscript(int studentId) {
        Student student = studentService.findStudentById(studentId);

        List<Enrollment> enrollments = getEnrollmentsForStudent(studentId);

        StringBuilder transcriptBuilder = new StringBuilder();
        transcriptBuilder.append("========================================\n");
        transcriptBuilder.append("         ACADEMIC TRANSCRIPT          \n");
        transcriptBuilder.append("========================================\n");
        transcriptBuilder.append(student.getProfile()).append("\n\n");
        transcriptBuilder.append("--- Enrolled Courses ---\n");

        if (enrollments.isEmpty()) {
            transcriptBuilder.append("No courses enrolled.\n");
        } else {
            for (Enrollment enrollment : enrollments) {
                transcriptBuilder.append(enrollment.toString()).append("\n");
            }
        }

        transcriptBuilder.append("\n--- Summary ---\n");
        double gpa = calculateGpa(enrollments);
        transcriptBuilder.append(String.format("Cumulative GPA: %.2f\n", gpa));
        transcriptBuilder.append("========================================\n");

        return transcriptBuilder.toString();
    }

    private List<Enrollment> getEnrollmentsForStudent(int studentId) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT * FROM enrollments WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            Student student = studentService.findStudentById(studentId);

            while (rs.next()) {
                Course course = courseService.findCourseByCode(new CourseCode(rs.getString("course_code")));
                Grade grade = Grade.valueOf(rs.getString("grade"));
                enrollments.add(new Enrollment(student, course, grade));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enrollments;
    }
    
    private double calculateGpa(List<Enrollment> enrollments) {
        double totalPoints = 0;
        int totalCredits = 0;

        for (Enrollment enrollment : enrollments) {
            if (enrollment.getGrade() != Grade.NA) {
                totalPoints += enrollment.getGrade().getPoints() * enrollment.getCourse().getCredits();
                totalCredits += enrollment.getCourse().getCredits();
            }
        }
        
        return (totalCredits == 0) ? 0.0 : totalPoints / totalCredits;
    }
}