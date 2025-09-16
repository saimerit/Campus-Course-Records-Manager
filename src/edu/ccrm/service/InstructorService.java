package edu.ccrm.service;

import edu.ccrm.domain.Instructor;
import edu.ccrm.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstructorService {
    private final DataStore dataStore = DataStore.getInstance();

    public void addInstructor(Instructor instructor) {
        dataStore.getInstructors().put(instructor.getId(), instructor);
    }

    public Instructor findInstructorById(int id) {
        return Optional.ofNullable(dataStore.getInstructors().get(id))
                .orElseThrow(() -> new RecordNotFoundException("Instructor with ID " + id + " not found."));
    }

    public List<Instructor> getAllInstructors() {
        return new ArrayList<>(dataStore.getInstructors().values());
    }
    
    public List<Instructor> getAllInstructorsSortedById() {
        return dataStore.getInstructors().values().stream()
                .sorted(Comparator.comparingInt(Instructor::getId))
                .collect(Collectors.toList());
    }
}