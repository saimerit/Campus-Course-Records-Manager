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

    public String generateTranscript(int studentId) throws RecordNotFoundException {
        Student student = studentService.findStudentById(studentId);
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsForStudent(studentId);

        StringBuilder transcript = new StringBuilder();
        transcript.append("--- Transcript for ").append(student.getFullName()).append(" ---\n");
        transcript.append("Student ID: ").append(student.getId()).append("\n");
        transcript.append("Registration Number: ").append(student.getRegNo()).append("\n");
        transcript.append("Status: ").append(student.getStatus()).append("\n\n");
        transcript.append("--- Enrolled Courses ---\n");

        if (enrollments.isEmpty()) {
            transcript.append("No courses enrolled.\n");
        } else {
            for (Enrollment enrollment : enrollments) {
                transcript.append("Course: ").append(enrollment.getCourse().getTitle());
                transcript.append(" (").append(enrollment.getCourse().getCourseCode().getCode()).append(")\n");
                transcript.append("Credits: ").append(enrollment.getCourse().getCredits()).append("\n");
                transcript.append("Grade: ").append(enrollment.getGrade() != null ? enrollment.getGrade() : "Not Graded").append("\n\n");
            }
        }
        transcript.append("--- End of Transcript ---");
        return transcript.toString();
    }
}