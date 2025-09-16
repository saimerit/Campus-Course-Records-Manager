package edu.ccrm.service;

import edu.ccrm.domain.Enrollment;
import edu.ccrm.domain.Grade;
import edu.ccrm.domain.Student;
import edu.ccrm.exception.RecordNotFoundException;
import java.util.List;

public class TranscriptService {
    private final DataStore dataStore = DataStore.getInstance();

    public String generateTranscript(int studentId) {
        Student student = dataStore.getStudents().get(studentId);
        if (student == null) {
            throw new RecordNotFoundException("Student with ID " + studentId + " not found.");
        }

        StringBuilder transcriptBuilder = new StringBuilder();
        transcriptBuilder.append("========================================\n");
        transcriptBuilder.append("         ACADEMIC TRANSCRIPT          \n");
        transcriptBuilder.append("========================================\n");
        transcriptBuilder.append(student.getProfile()).append("\n\n");
        transcriptBuilder.append("--- Enrolled Courses ---\n");

        List<Enrollment> enrollments = student.getEnrollments();
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

