package edu.ccrm.service;

import edu.ccrm.domain.Name;
import edu.ccrm.domain.Student;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    public StudentService() {}

    public void addStudent(Student student) throws DataIntegrityException {
        try (Connection conn = DatabaseManager.getConnection()) {
            addStudent(student, conn);
        } catch (SQLException e) {
            throw new DataIntegrityException("Database error adding student: " + e.getMessage(), e);
        }
    }

    public void addStudent(Student student, Connection conn) throws DataIntegrityException {
        String sql = "INSERT INTO students (id, reg_no, first_name, last_name, email, status, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getRegNo());
            pstmt.setString(3, student.getFullName().getFirstName());
            pstmt.setString(4, student.getFullName().getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getStatus().name());
            pstmt.setDate(7, Date.valueOf(student.getRegistrationDate()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DataIntegrityException("Student with ID " + student.getId() + " or Registration No. "
                        + student.getRegNo() + " already exists.", e);
            }
            throw new DataIntegrityException("Error adding student: " + e.getMessage(), e);
        }
    }

    public List<Student> getAllStudentsSortedById() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all students: " + e.getMessage());
        }
        return students;
    }

    public Student findStudentByRegNo(String regNo) throws RecordNotFoundException {
        String sql = "SELECT * FROM students WHERE reg_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, regNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStudent(rs);
                } else {
                    throw new RecordNotFoundException("Student with Reg No. " + regNo + " not found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error finding student by Reg No.: " + e.getMessage());
            throw new RecordNotFoundException("Database error finding student by Reg No.: " + e.getMessage());
        }
    }

    public void updateStudentStatus(String regNo, Student.Status newStatus) throws RecordNotFoundException {
        String sql = "UPDATE students SET status = ? WHERE reg_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, regNo);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with Reg No. " + regNo + " not found.");
            }
        } catch (SQLException e) {
            System.err.println("Database error updating student status: " + e.getMessage());
            throw new RecordNotFoundException("Database error updating student status: " + e.getMessage());
        }
    }

    public void updateStudent(Student student) throws RecordNotFoundException, DataIntegrityException {
        String sql = "UPDATE students SET first_name = ?, last_name = ?, email = ? WHERE reg_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getFullName().getFirstName());
            pstmt.setString(2, student.getFullName().getLastName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getRegNo());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with Reg No. " + student.getRegNo() + " not found.");
            }
        } catch (SQLException e) {
            throw new DataIntegrityException("Database error updating student: " + e.getMessage(), e);
        }
    }


    public Student mapRowToStudent(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String regNo = rs.getString("reg_no");
        Name name = new Name(rs.getString("first_name"), rs.getString("last_name"));
        String email = rs.getString("email");
        Student.Status status = Student.Status.valueOf(rs.getString("status"));
        LocalDate registrationDate = rs.getDate("registration_date").toLocalDate();
        return new Student(id, regNo, name, email, status, registrationDate);
    }
}