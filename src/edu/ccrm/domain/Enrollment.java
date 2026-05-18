package edu.ccrm.domain;

import java.time.LocalDate;

public class Enrollment {
    private Student student;
    private Course course;
    private LocalDate enrollmentDate;
    private Grade grade;
    private int enrollmentYear;
    private String enrollmentSemester;

    public Enrollment(Student student, Course course) {
        this.student = student;
        this.course = course;
        this.enrollmentDate = LocalDate.now();
        this.enrollmentYear = LocalDate.now().getYear();
        this.enrollmentSemester = course.getSemester() != null ? course.getSemester().name() : "";
        this.grade = Grade.NA;
    }

    public Enrollment(Student student, Course course, Grade grade) {
        this.student = student;
        this.course = course;
        this.enrollmentDate = LocalDate.now();
        this.enrollmentYear = LocalDate.now().getYear();
        this.enrollmentSemester = course.getSemester() != null ? course.getSemester().name() : "";
        this.grade = grade;
    }

    public Enrollment(Student student, Course course, Grade grade, int enrollmentYear) {
        this.student = student;
        this.course = course;
        this.enrollmentDate = LocalDate.now();
        this.enrollmentYear = enrollmentYear;
        this.enrollmentSemester = course.getSemester() != null ? course.getSemester().name() : "";
        this.grade = grade;
    }

    public Enrollment(Student student, Course course, Grade grade, int enrollmentYear, String enrollmentSemester) {
        this.student = student;
        this.course = course;
        this.enrollmentDate = LocalDate.now();
        this.enrollmentYear = enrollmentYear;
        this.enrollmentSemester = enrollmentSemester != null ? enrollmentSemester : "";
        this.grade = grade;
    }

    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }
    public int getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(int enrollmentYear) { this.enrollmentYear = enrollmentYear; }
    public String getEnrollmentSemester() { return enrollmentSemester; }
    public void setEnrollmentSemester(String enrollmentSemester) { this.enrollmentSemester = enrollmentSemester; }

    @Override
    public String toString() {
        return String.format("Enrollment | Course: %s, Grade: %s (%.1f), %s %d",
                course.getCourseCode(), grade.name(), grade.getPoints(), enrollmentSemester, enrollmentYear);
    }
}
