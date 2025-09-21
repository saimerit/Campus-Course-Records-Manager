package edu.ccrm.service;

import edu.ccrm.domain.Instructor;
import edu.ccrm.domain.Name;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.exception.RecordNotFoundException;
import edu.ccrm.io.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorService {

    public InstructorService() {}

    public void addInstructor(Instructor instructor) throws DataIntegrityException {
        try (Connection conn = DatabaseManager.getConnection()) {
            addInstructor(instructor, conn);
        } catch (SQLException e) {
            throw new DataIntegrityException("Database error adding instructor: " + e.getMessage(), e);
        }
    }

    public void addInstructor(Instructor instructor, Connection conn) throws DataIntegrityException {
        String sql = "INSERT INTO instructors (id, first_name, last_name, email, department) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructor.getId());
            pstmt.setString(2, instructor.getFullName().getFirstName());
            pstmt.setString(3, instructor.getFullName().getLastName());
            pstmt.setString(4, instructor.getEmail());
            pstmt.setString(5, instructor.getDepartment());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DataIntegrityException("Instructor with ID " + instructor.getId() + " already exists.", e);
            }
            throw new DataIntegrityException("Error adding instructor: " + e.getMessage(), e);
        }
    }

    public List<Instructor> getAllInstructorsSortedById() {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT * FROM instructors ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                instructors.add(mapRowToInstructor(rs));
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all instructors: " + e.getMessage());
        }
        return instructors;
    }

    public Instructor findInstructorById(int instructorId) throws RecordNotFoundException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return findInstructorById(instructorId, conn);
        } catch (SQLException e) {
            throw new RecordNotFoundException("Database error finding instructor: " + e.getMessage());
        }
    }

    public Instructor findInstructorById(int instructorId, Connection conn) throws RecordNotFoundException {
        String sql = "SELECT * FROM instructors WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInstructor(rs);
                } else {
                    throw new RecordNotFoundException("Instructor with ID " + instructorId + " not found.");
                }
            }
        } catch (SQLException e) {
            throw new RecordNotFoundException("Database error finding instructor by ID: " + e.getMessage(), e);
        }
    }
    
    public void updateInstructor(Instructor instructor) throws RecordNotFoundException, DataIntegrityException {
        String sql = "UPDATE instructors SET first_name = ?, last_name = ?, email = ?, department = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, instructor.getFullName().getFirstName());
            pstmt.setString(2, instructor.getFullName().getLastName());
            pstmt.setString(3, instructor.getEmail());
            pstmt.setString(4, instructor.getDepartment());
            pstmt.setInt(5, instructor.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Instructor with ID " + instructor.getId() + " not found.");
            }
        } catch (SQLException e) {
            throw new DataIntegrityException("Database error updating instructor: " + e.getMessage(), e);
        }
    }

    private Instructor mapRowToInstructor(ResultSet rs) throws SQLException {
        return new Instructor(
                rs.getInt("id"),
                new Name(rs.getString("first_name"), rs.getString("last_name")),
                rs.getString("email"),
                rs.getString("department")
        );
    }
}