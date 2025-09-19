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

    public void addInstructor(Instructor instructor) {
        if (instructorExists(instructor.getId())) {
            throw new DataIntegrityException("Instructor with ID " + instructor.getId() + " already exists.");
        }
        String sql = "INSERT INTO instructors (id, first_name, last_name, email, department) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructor.getId());
            pstmt.setString(2, instructor.getFullName().getFirstName());
            pstmt.setString(3, instructor.getFullName().getLastName());
            pstmt.setString(4, instructor.getEmail());
            pstmt.setString(5, instructor.getDepartment());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Instructor findInstructorById(int id) {
        String sql = "SELECT * FROM instructors WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Instructor(
                        rs.getInt("id"),
                        new Name(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("email"),
                        rs.getString("department")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RecordNotFoundException("Instructor with ID " + id + " not found.");
    }

    public List<Instructor> getAllInstructorsSortedById() {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT * FROM instructors ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Instructor instructor = new Instructor(
                        rs.getInt("id"),
                        new Name(rs.getString("first_name"), rs.getString("last_name")),
                        rs.getString("email"),
                        rs.getString("department")
                );
                instructors.add(instructor);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instructors;
    }

    private boolean instructorExists(int id) {
        String sql = "SELECT COUNT(*) FROM instructors WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
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