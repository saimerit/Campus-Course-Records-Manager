package edu.ccrm.service;

import edu.ccrm.domain.Name;
import edu.ccrm.domain.Student;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    public void addStudent(Student student) {
        // This SQL now matches the columns in your database_setup.sql file
        String sql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getRegNo());
            pstmt.setString(3, student.getFullName().getFirstName());
            pstmt.setString(4, student.getFullName().getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getStatus().name());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateStudentStatus(int studentId, Student.Status newStatus) throws RecordNotFoundException {
        // Corrected column name to 'id'
        String sql = "UPDATE students SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, studentId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with ID " + studentId + " not found.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating student status: " + e.getMessage());
        }
    }

    public List<Student> getAllStudentsSortedById() {
        List<Student> students = new ArrayList<>();
        // Corrected SQL column names to match the database
        String sql = "SELECT id, reg_no, first_name, last_name, email, status FROM students ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String regNo = rs.getString("reg_no");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                Student.Status status = Student.Status.valueOf(rs.getString("status"));

                Student student = new Student(id, regNo, new Name(firstName, lastName), email);
                student.setStatus(status);
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving students: " + e.getMessage());
        }
        return students;
    }

    public Student findStudentById(int studentId) throws RecordNotFoundException {
        // Corrected column name to 'id'
        String sql = "SELECT * FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Student(rs.getInt("id"), rs.getString("reg_no"), new Name(rs.getString("first_name"), rs.getString("last_name")), rs.getString("email"));
            } else {
                throw new RecordNotFoundException("Student with ID " + studentId + " not found.");
            }
        } catch (SQLException e) {
           System.err.println("Error finding student by ID: " + e.getMessage());
        }
        return null;
    }
}