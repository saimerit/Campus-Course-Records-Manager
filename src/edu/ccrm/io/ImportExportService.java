package edu.ccrm.io;

import edu.ccrm.config.AppConfig;
import edu.ccrm.domain.*;
import edu.ccrm.service.CourseService;
import edu.ccrm.service.InstructorService;
import edu.ccrm.service.StudentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportExportService {

    private final AppConfig config = AppConfig.getInstance();
    private final StudentService studentService = new StudentService();
    private final InstructorService instructorService = new InstructorService();
    private final CourseService courseService = new CourseService();

    public void importStudents(Path filePath) throws IOException {
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.skip(1).forEach(line -> {
                String[] parts = line.split(",");
                Student student = new Student(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        new Name(parts[2], parts[3]),
                        parts[4],
                        Student.Status.valueOf(parts[5]),
                        LocalDate.parse(parts[6])
                );
                studentService.addStudent(student);
            });
        }
    }

    public void importInstructors(Path filePath) throws IOException {
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.skip(1).forEach(line -> {
                String[] parts = line.split(",");
                Instructor instructor = new Instructor(
                        Integer.parseInt(parts[0]),
                        new Name(parts[1], parts[2]),
                        parts[3],
                        parts[4]
                );
                instructorService.addInstructor(instructor);
            });
        }
    }

    public void importCourses(Path filePath) throws IOException {
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.skip(1).forEach(line -> {
                String[] parts = line.split(",");
                CourseCode courseCode = new CourseCode(parts[0]);
                Instructor instructor = instructorService.findInstructorById(Integer.parseInt(parts[4]));
                Course course = new Course.Builder(courseCode)
                        .withTitle(parts[1])
                        .withCredits(Integer.parseInt(parts[2]))
                        .withDepartment(parts[3])
                        .withInstructor(instructor)
                        .withSemester(Semester.valueOf(parts[5]))
                        .build();
                courseService.addCourse(course);
            });
        }
    }
    
    public void exportData() throws IOException {
        Files.createDirectories(config.getDataDirectory());
        
        System.out.println("Exporting data from database...");
        exportStudents(config.getStudentsFilePath());
        exportInstructors(config.getInstructorsFilePath());
        exportCourses(config.getCoursesFilePath());
        
        System.out.println("Data exported successfully.");
    }
    
    private void exportStudents(Path path) throws IOException {
        List<String> lines = studentService.getAllStudentsSortedById().stream()
            .map(Student::toCsvString)
            .collect(Collectors.toList());
        lines.add(0, "id,regNo,firstName,lastName,email,status,registrationDate");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private void exportInstructors(Path path) throws IOException {
        List<String> lines = instructorService.getAllInstructorsSortedById().stream()
            .map(Instructor::toCsvString)
            .collect(Collectors.toList());
        lines.add(0, "id,firstName,lastName,email,department");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void exportCourses(Path path) throws IOException {
        List<String> lines = courseService.getAllCoursesSortedByCode().stream()
            .map(c -> String.join(",",
                c.getCourseCode().getCode(),
                c.getTitle(),
                String.valueOf(c.getCredits()),
                c.getDepartment(),
                (c.getInstructor() != null) ? String.valueOf(c.getInstructor().getId()) : "",
                c.getSemester().name()
            ))
            .collect(Collectors.toList());
        lines.add(0, "code,title,credits,department,instructorId,semester");
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}