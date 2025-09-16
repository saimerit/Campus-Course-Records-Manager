package edu.ccrm.domain;

public class Course {
    private final CourseCode courseCode;
    private String title;
    private int credits;
    private Instructor instructor;
    private Semester semester;
    private String department;
    private boolean active;

    private Course(Builder builder) {
        this.courseCode = builder.courseCode;
        this.title = builder.title;
        this.credits = builder.credits;
        this.instructor = builder.instructor;
        this.semester = builder.semester;
        this.department = builder.department;
        this.active = true;
    }

    public CourseCode getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        String instructorName = (instructor != null) ? instructor.getFullName().toString() : "Not Assigned";
        return String.format("Course | Code: %s, Title: %s, Credits: %d, Dept: %s, Semester: %s, Instructor: %s, Status: %s",
                courseCode, title, credits, department, semester, instructorName, active ? "Active" : "Inactive");
    }

    public static class Builder {
        private final CourseCode courseCode;
        private String title;
        private int credits;
        private Instructor instructor;
        private Semester semester;
        private String department;

        public Builder(CourseCode courseCode) {
            if (courseCode == null) {
                throw new IllegalArgumentException("Course code cannot be null");
            }
            this.courseCode = courseCode;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withCredits(int credits) {
            this.credits = credits;
            return this;
        }

        public Builder withInstructor(Instructor instructor) {
            this.instructor = instructor;
            return this;
        }

        public Builder withSemester(Semester semester) {
            this.semester = semester;
            return this;
        }

        public Builder withDepartment(String department) {
            this.department = department;
            return this;
        }

        public Course build() {
            return new Course(this);
        }
    }
}
