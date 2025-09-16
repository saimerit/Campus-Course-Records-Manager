package edu.ccrm.service;

import edu.ccrm.domain.*;
import edu.ccrm.exception.DuplicateEnrollmentException;
import edu.ccrm.exception.MaxCreditLimitExceededException;
import edu.ccrm.exception.RecordNotFoundException;
import java.util.Optional;

public class EnrollmentService {
    private final DataStore dataStore = DataStore.getInstance();
    private static final int MAX_CREDITS_PER_SEMESTER = 20;

    public void enrollStudent(int studentId, CourseCode courseCode)
            throws DuplicateEnrollmentException, MaxCreditLimitExceededException {
        Student student = Optional.ofNullable(dataStore.getStudents().get(studentId))
                .orElseThrow(() -> new RecordNotFoundException("Student not found."));

        Course course = Optional.ofNullable(dataStore.getCourses().get(courseCode))
                .orElseThrow(() -> new RecordNotFoundException("Course not found."));

        boolean alreadyEnrolled = student.getEnrollments().stream()
                .anyMatch(e -> e.getCourse().getCourseCode().equals(courseCode));
        if (alreadyEnrolled) {
            throw new DuplicateEnrollmentException("Student is already enrolled in this course.");
        }

        int currentCredits = student.getEnrollments().stream()
                .filter(e -> e.getCourse().getSemester() == course.getSemester())
                .mapToInt(e -> e.getCourse().getCredits())
                .sum();
        if (currentCredits + course.getCredits() > MAX_CREDITS_PER_SEMESTER) {
            throw new MaxCreditLimitExceededException("Enrollment exceeds max credit limit for the semester.");
        }

        Enrollment enrollment = new Enrollment(student, course);
        student.addEnrollment(enrollment);
    }

    public void unenrollStudent(int studentId, CourseCode courseCode) {
        Student student = Optional.ofNullable(dataStore.getStudents().get(studentId))
                .orElseThrow(() -> new RecordNotFoundException("Student not found."));
        if (!student.removeEnrollment(courseCode)) {
            throw new RecordNotFoundException("Enrollment record not found for this student and course.");
        }
    }
    
    public void recordGrade(int studentId, CourseCode courseCode, Grade grade) {
        Student student = Optional.ofNullable(dataStore.getStudents().get(studentId))
                .orElseThrow(() -> new RecordNotFoundException("Student not found."));
        
        student.getEnrollments().stream()
            .filter(e -> e.getCourse().getCourseCode().equals(courseCode))
            .findFirst()
            .orElseThrow(() -> new RecordNotFoundException("Student is not enrolled in this course."))
            .setGrade(grade);
    }
}

