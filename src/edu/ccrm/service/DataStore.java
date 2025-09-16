package edu.ccrm.service;

import edu.ccrm.domain.Course;
import edu.ccrm.domain.CourseCode;
import edu.ccrm.domain.Instructor;
import edu.ccrm.domain.Student;

import java.util.HashMap;
import java.util.Map;

public class DataStore {
    private static DataStore instance;

    private final Map<Integer, Student> students = new HashMap<>();
    private final Map<CourseCode, Course> courses = new HashMap<>();
    private final Map<Integer, Instructor> instructors = new HashMap<>();

    private DataStore() {}

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Map<Integer, Student> getStudents() {
        return students;
    }

    public Map<CourseCode, Course> getCourses() {
        return courses;
    }

    public Map<Integer, Instructor> getInstructors() {
        return instructors;
    }
}

