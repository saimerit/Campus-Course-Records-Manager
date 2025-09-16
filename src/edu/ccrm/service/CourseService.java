package edu.ccrm.service;

import edu.ccrm.domain.Course;
import edu.ccrm.domain.CourseCode;
import edu.ccrm.domain.Instructor;
import edu.ccrm.domain.Semester;
import edu.ccrm.exception.RecordNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CourseService {
    private final DataStore dataStore = DataStore.getInstance();

    public void addCourse(Course course) {
        dataStore.getCourses().put(course.getCourseCode(), course);
    }

    public Course findCourseByCode(CourseCode courseCode) {
        Course course = dataStore.getCourses().get(courseCode);
        if (course == null) {
            throw new RecordNotFoundException("Course with code " + courseCode + " not found.");
        }
        return course;
    }
    
    public void assignInstructor(CourseCode courseCode, int instructorId) {
        Course course = findCourseByCode(courseCode);
        Instructor instructor = dataStore.getInstructors().get(instructorId);
        if (instructor == null) {
            throw new RecordNotFoundException("Instructor with ID " + instructorId + " not found.");
        }
        course.setInstructor(instructor);
    }

    public List<Course> getAllCoursesSortedByCode() {
        return dataStore.getCourses().values().stream()
                .sorted(Comparator.comparing(c -> c.getCourseCode().getCode()))
                .collect(Collectors.toList());
    }

    public List<Course> filterCourses(Predicate<Course> predicate) {
        return dataStore.getCourses().values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public Predicate<Course> byInstructor(int instructorId) {
        return course -> course.getInstructor() != null && course.getInstructor().getId() == instructorId;
    }
    
    public Predicate<Course> byDepartment(String department) {
        return course -> course.getDepartment().equalsIgnoreCase(department);
    }
    
    public Predicate<Course> bySemester(Semester semester) {
        return course -> course.getSemester() == semester;
    }
}

