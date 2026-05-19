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
        transcript.append("Transcript for ").append(student.getFullName()).append("\n");
        transcript.append("ID: ").append(student.getId()).append(" | Reg No: ").append(student.getRegNo()).append("\n");
        transcript.append("Status: ").append(student.getStatus()).append("\n\n");

        if (enrollments.isEmpty()) {
            transcript.append("No enrollment records found.\n");
        } else {
            // Group enrollments by year and semester
            java.util.Map<SemesterTerm, List<Enrollment>> grouped = new java.util.TreeMap<>();
            for (Enrollment enrollment : enrollments) {
                SemesterTerm term = new SemesterTerm(enrollment.getEnrollmentYear(), enrollment.getEnrollmentSemester());
                grouped.computeIfAbsent(term, k -> new java.util.ArrayList<>()).add(enrollment);
            }

            double runningTotalPoints = 0;
            int runningTotalCredits = 0;

            for (java.util.Map.Entry<SemesterTerm, List<Enrollment>> entry : grouped.entrySet()) {
                SemesterTerm term = entry.getKey();
                List<Enrollment> semEnrollments = entry.getValue();

                transcript.append("[").append(term.getYear()).append(" ").append(term.getSemester()).append("]\n");
                transcript.append(String.format("%-10s | %-35s | %-7s | %-10s\n", "Code", "Title", "Credits", "Grade"));
                transcript.append("---------------------------------------------------------------------\n");

                double semesterPoints = 0;
                int semesterCredits = 0;

                for (Enrollment e : semEnrollments) {
                    transcript.append(String.format("%-10s | %-35s | %-7d | %-10s\n",
                            e.getCourse().getCourseCode().getCode(),
                            e.getCourse().getTitle(),
                            e.getCourse().getCredits(),
                            e.getGrade() != null ? e.getGrade().toString() : "N/A"));

                    if (e.getGrade() != null && e.getGrade() != edu.ccrm.domain.Grade.NA) {
                        double pts = e.getGrade().getPoints() * e.getCourse().getCredits();
                        semesterPoints += pts;
                        semesterCredits += e.getCourse().getCredits();

                        runningTotalPoints += pts;
                        runningTotalCredits += e.getCourse().getCredits();
                    }
                }

                double semesterGpa = semesterCredits == 0 ? 0.0 : semesterPoints / semesterCredits;
                double cumulativeGpa = runningTotalCredits == 0 ? 0.0 : runningTotalPoints / runningTotalCredits;

                transcript.append(String.format("Semester GPA: %.2f | Cumulative CGPA: %.2f\n\n", semesterGpa, cumulativeGpa));
            }
        }
        return transcript.toString();
    }

    public double calculateCGPA(List<Enrollment> enrollments) {
        double totalPoints = 0;
        int totalCredits = 0;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getGrade() != null && enrollment.getGrade() != edu.ccrm.domain.Grade.NA) {
                totalPoints += enrollment.getGrade().getPoints() * enrollment.getCourse().getCredits();
                totalCredits += enrollment.getCourse().getCredits();
            }
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    private static class SemesterTerm implements Comparable<SemesterTerm> {
        private final int year;
        private final String semester;

        public SemesterTerm(int year, String semester) {
            this.year = year;
            this.semester = semester != null ? semester.toUpperCase() : "SPRING";
        }

        public int getYear() { return year; }
        public String getSemester() { return semester; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SemesterTerm)) return false;
            SemesterTerm that = (SemesterTerm) o;
            return year == that.year && java.util.Objects.equals(semester, that.semester);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(year, semester);
        }

        @Override
        public int compareTo(SemesterTerm o) {
            if (this.year != o.year) {
                return Integer.compare(this.year, o.year);
            }
            return Integer.compare(getSemesterOrdinal(this.semester), getSemesterOrdinal(o.semester));
        }

        private int getSemesterOrdinal(String sem) {
            try {
                return edu.ccrm.domain.Semester.valueOf(sem).ordinal();
            } catch (Exception e) {
                return 99;
            }
        }
    }
}