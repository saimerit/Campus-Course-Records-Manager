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
        String sql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getRegNo());
            pstmt.setString(3, student.getFullName().getFirstName());
            pstmt.setString(4, student.getFullName().getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getStatus().name());
            pstmt.setDate(7, Date.valueOf(student.getRegistrationDate()));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Student findStudentById(int id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Student(
                        rs.getInt("id"),
                        rs.getString("reg_no"),
                        new Name(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("email"),
                        Student.Status.valueOf(rs.getString("status")),
                        rs.getDate("registration_date").toLocalDate()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RecordNotFoundException("Student with ID " + id + " not found.");
    }

    public List<Student> getAllStudentsSortedById() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Student student = new Student(
                        rs.getInt("id"),
                        rs.getString("reg_no"),
                        new Name(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("email"),
                        Student.Status.valueOf(rs.getString("status")),
                        rs.getDate("registration_date").toLocalDate()
                );
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public void updateStudentStatus(int id, Student.Status status) {
        String sql = "UPDATE students SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setInt(2, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with ID " + id + " not found for update.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}