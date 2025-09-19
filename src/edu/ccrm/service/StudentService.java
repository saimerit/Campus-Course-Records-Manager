package edu.ccrm.service;

import edu.ccrm.domain.Name;
import edu.ccrm.domain.Student;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    public void addStudent(Student student) {
        if (studentExists(student.getId(), student.getRegNo())) {
            throw new DataIntegrityException("Student with ID " + student.getId() + " or registration number " + student.getRegNo() + " already exists.");
        }
        String sql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getRegNo());
            pstmt.setString(3, student.getFullName().getFirstName());
            pstmt.setString(4, student.getFullName().getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getStatus().name());
            pstmt.setDate(7, java.sql.Date.valueOf(student.getRegistrationDate()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateStudentStatus(int studentId, Student.Status newStatus) throws RecordNotFoundException {
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
        String sql = "SELECT id, reg_no, first_name, last_name, email, status, registration_date FROM students ORDER BY id";

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
            System.err.println("Error retrieving students: " + e.getMessage());
        }
        return students;
    }

    public Student findStudentById(int studentId) throws RecordNotFoundException {
        String sql = "SELECT id, reg_no, first_name, last_name, email, status, registration_date FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
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
            } else {
                throw new RecordNotFoundException("Student with ID " + studentId + " not found.");
            }
        } catch (SQLException e) {
           System.err.println("Error finding student by ID: " + e.getMessage());
        }
        return null;
    }

    private boolean studentExists(int id, String regNo) {
        String sql = "SELECT COUNT(*) FROM students WHERE id = ? OR reg_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, regNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}