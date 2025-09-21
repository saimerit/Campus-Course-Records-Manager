package edu.ccrm.service;

import edu.ccrm.domain.Enrollment;
import edu.ccrm.domain.Student;
import edu.ccrm.exception.RecordNotFoundException;

import java.util.List;

public class TranscriptService {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    public TranscriptService(StudentService studentService, EnrollmentService enrollmentService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
    }

    public String generateTranscript(String studentRegNo) throws RecordNotFoundException {
        Student student = studentService.findStudentByRegNo(studentRegNo);
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(studentRegNo);

        StringBuilder transcript = new StringBuilder();
        transcript.append("--- Transcript for ").append(student.getFullName()).append(" ---\n");
        transcript.append("Student ID: ").append(student.getId()).append("\n");
        transcript.append("Registration Number: ").append(student.getRegNo()).append("\n");
        transcript.append("Status: ").append(student.getStatus()).append("\n\n");
        transcript.append("--- Enrolled Courses ---\n");

        if (enrollments.isEmpty()) {
            transcript.append("No courses enrolled.\n");
        } else {
            transcript.append(String.format("%-10s | %-40s | %-10s | %-10s\n", "Code", "Title", "Credits", "Grade"));
            transcript.append("-----------------------------------------------------------------------------\n");

            for (Enrollment enrollment : enrollments) {
                transcript.append(String.format("%-10s | %-40s | %-10d | %-10s\n",
                        enrollment.getCourse().getCourseCode().getCode(),
                        enrollment.getCourse().getTitle(),
                        enrollment.getCourse().getCredits(),
                        enrollment.getGrade() != null ? enrollment.getGrade() : "Not Graded"));
            }

            double cgpa = calculateCGPA(enrollments);
            transcript.append(String.format("\nCumulative Grade Point Average (CGPA): %.2f\n", cgpa));
        }
        transcript.append("\n--- End of Transcript ---");
        return transcript.toString();
    }

    private double calculateCGPA(List<Enrollment> enrollments) {
        double totalPoints = 0;
        int totalCredits = 0;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getGrade() != null) {
                totalPoints += enrollment.getGrade().getPoints() * enrollment.getCourse().getCredits();
                totalCredits += enrollment.getCourse().getCredits();
            }
        }
        return totalCredits == 0 ? 0 : totalPoints / totalCredits;
    }
}