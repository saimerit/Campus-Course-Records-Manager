package edu.ccrm.service;

import edu.ccrm.domain.Student;
import edu.ccrm.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentService {
    private final DataStore dataStore = DataStore.getInstance();

    public void addStudent(Student student) {
        dataStore.getStudents().put(student.getId(), student);
    }

    public Student findStudentById(int id) {
        return Optional.ofNullable(dataStore.getStudents().get(id))
                .orElseThrow(() -> new RecordNotFoundException("Student with ID " + id + " not found."));
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(dataStore.getStudents().values());
    }
    
    public List<Student> getAllStudentsSortedById() {
        return dataStore.getStudents().values().stream()
                .sorted(Comparator.comparingInt(Student::getId))
                .collect(Collectors.toList());
    }

    public void updateStudentStatus(int id, Student.Status status) {
        Student student = findStudentById(id);
        student.setStatus(status);
    }
}

