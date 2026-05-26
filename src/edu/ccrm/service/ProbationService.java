package edu.ccrm.service;

import edu.ccrm.domain.ProbationReport;
import edu.ccrm.exception.DataIntegrityException;
import edu.ccrm.io.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProbationService {

    public ProbationService() {}

    /**
     * Saves a new probation report to the database, associating it with multiple students,
     * and sets those students' statuses to PROBATION. Executed in a transaction.
     * @param report the probation report to add
     * @throws DataIntegrityException if a database error or constraint violation occurs
     */
    public void addProbationReport(ProbationReport report) throws DataIntegrityException {
        String insertReportSql = "INSERT INTO probation_reports (probation_id, start_date, end_date, reason) VALUES (?, ?, ?, ?)";
        String insertStudentSql = "INSERT INTO probation_students (probation_id, student_reg_no) VALUES (?, ?)";
        String updateStudentStatusSql = "UPDATE students SET status = 'PROBATION', probation_count = probation_count + 1 WHERE reg_no = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtReport = conn.prepareStatement(insertReportSql);
                 PreparedStatement pstmtStudent = conn.prepareStatement(insertStudentSql);
                 PreparedStatement pstmtStatus = conn.prepareStatement(updateStudentStatusSql)) {
                
                pstmtReport.setString(1, report.getProbationId());
                pstmtReport.setDate(2, Date.valueOf(report.getStartDate()));
                pstmtReport.setDate(3, Date.valueOf(report.getEndDate()));
                pstmtReport.setString(4, report.getReason());
                pstmtReport.executeUpdate();
                
                for (String regNo : report.getStudentRegNos()) {
                    pstmtStudent.setString(1, report.getProbationId());
                    pstmtStudent.setString(2, regNo);
                    pstmtStudent.executeUpdate();
                    
                    pstmtStatus.setString(1, regNo);
                    pstmtStatus.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || (e.getErrorCode() == 1)) {
                throw new DataIntegrityException("A probation report with ID " + report.getProbationId() + " already exists.", e);
            }
            throw new DataIntegrityException("Error adding probation report: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves all probation reports for a given student registration number, sorted by start date.
     * @param regNo student registration number
     * @return list of probation reports
     */
    public List<ProbationReport> getProbationReportsForStudent(String regNo) {
        List<ProbationReport> reports = new ArrayList<>();
        String sql = "SELECT r.* FROM probation_reports r JOIN probation_students s ON r.probation_id = s.probation_id "
                   + "WHERE s.student_reg_no = ? ORDER BY r.start_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, regNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("probation_id");
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    String reason = rs.getString("reason");
                    reports.add(new ProbationReport(id, start, end, reason, List.of(regNo)));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching probation reports for student " + regNo + ": " + e.getMessage());
        }
        return reports;
    }

    /**
     * Retrieves a list of student registration numbers associated with a given probation report ID.
     * @param probationId probation report ID
     * @return list of student registration numbers
     */
    public List<String> getStudentsForProbationReport(String probationId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getStudentsForProbationReport(probationId, conn);
        } catch (SQLException e) {
            System.err.println("Database error fetching students for probation " + probationId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a list of student registration numbers associated with a given probation report ID using an active connection.
     * @param probationId probation report ID
     * @param conn database connection
     * @return list of student registration numbers
     * @throws SQLException on database errors
     */
    public List<String> getStudentsForProbationReport(String probationId, Connection conn) throws SQLException {
        List<String> regNos = new ArrayList<>();
        String sql = "SELECT student_reg_no FROM probation_students WHERE probation_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, probationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    regNos.add(rs.getString("student_reg_no"));
                }
            }
        }
        return regNos;
    }

    /**
     * Retrieves all probation reports from the database.
     * @return list of all probation reports
     */
    public List<ProbationReport> getAllProbationReports() {
        List<ProbationReport> reports = new ArrayList<>();
        String sql = "SELECT * FROM probation_reports ORDER BY start_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String id = rs.getString("probation_id");
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end = rs.getDate("end_date").toLocalDate();
                String reason = rs.getString("reason");
                List<String> students = getStudentsForProbationReport(id, conn);
                reports.add(new ProbationReport(id, start, end, reason, students));
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching all probation reports: " + e.getMessage());
        }
        return reports;
    }

    /**
     * Deletes a probation report by its ID, decrementing the probation count of all involved
     * students and resetting their status to ACTIVE if they have no other probation reports.
     * @param probationId the ID of the probation report to delete
     * @throws DataIntegrityException if a database error occurs
     */
    public void deleteProbationReport(String probationId) throws DataIntegrityException {
        String deleteReportSql = "DELETE FROM probation_reports WHERE probation_id = ?";
        String checkOtherProbationsSql = "SELECT COUNT(*) FROM probation_students WHERE student_reg_no = ? AND probation_id != ?";
        String updateStudentSql = "UPDATE students SET status = 'ACTIVE' WHERE reg_no = ?";
        String decrementCountSql = "UPDATE students SET probation_count = GREATEST(0, probation_count - 1) WHERE reg_no = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            List<String> students = getStudentsForProbationReport(probationId, conn);

            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteReportSql);
                 PreparedStatement pstmtCheck = conn.prepareStatement(checkOtherProbationsSql);
                 PreparedStatement pstmtUpdate = conn.prepareStatement(updateStudentSql);
                 PreparedStatement pstmtDecrement = conn.prepareStatement(decrementCountSql)) {

                // 1. Decrement probation count for all students in this report
                for (String regNo : students) {
                    pstmtDecrement.setString(1, regNo);
                    pstmtDecrement.executeUpdate();
                }

                // 2. Delete the report (cascade deletes joint table rows in PROBATION_STUDENTS)
                pstmtDelete.setString(1, probationId);
                pstmtDelete.executeUpdate();

                // 3. For each student, check if they have other probation reports. If not, reset status to ACTIVE
                for (String regNo : students) {
                    pstmtCheck.setString(1, regNo);
                    pstmtCheck.setString(2, probationId);
                    try (ResultSet rs = pstmtCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            // Reset status to ACTIVE
                            pstmtUpdate.setString(1, regNo);
                            pstmtUpdate.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataIntegrityException("Error deleting probation report: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Updates an existing probation report. Handles adding new students and removing old ones,
     * decrementing probation count and restoring status for removed students, and incrementing
     * count and updating status to PROBATION for added students.
     * @param report the updated probation report
     * @throws DataIntegrityException if a database error occurs
     */
    public void updateProbationReport(ProbationReport report) throws DataIntegrityException {
        String updateReportSql = "UPDATE probation_reports SET start_date = ?, end_date = ?, reason = ? WHERE probation_id = ?";
        String deleteStudentSql = "DELETE FROM probation_students WHERE probation_id = ? AND student_reg_no = ?";
        String insertStudentSql = "INSERT INTO probation_students (probation_id, student_reg_no) VALUES (?, ?)";
        String decrementCountSql = "UPDATE students SET probation_count = GREATEST(0, probation_count - 1) WHERE reg_no = ?";
        String incrementCountSql = "UPDATE students SET status = 'PROBATION', probation_count = probation_count + 1 WHERE reg_no = ?";
        String checkOtherProbationsSql = "SELECT COUNT(*) FROM probation_students WHERE student_reg_no = ?";
        String updateStudentActiveSql = "UPDATE students SET status = 'ACTIVE' WHERE reg_no = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Get old student list from DB before updating
            List<String> oldStudents = getStudentsForProbationReport(report.getProbationId(), conn);
            List<String> newStudents = report.getStudentRegNos();

            List<String> removedStudents = new ArrayList<>(oldStudents);
            removedStudents.removeAll(newStudents);

            List<String> addedStudents = new ArrayList<>(newStudents);
            addedStudents.removeAll(oldStudents);

            try (PreparedStatement pstmtUpdateReport = conn.prepareStatement(updateReportSql);
                 PreparedStatement pstmtDeleteStudent = conn.prepareStatement(deleteStudentSql);
                 PreparedStatement pstmtInsertStudent = conn.prepareStatement(insertStudentSql);
                 PreparedStatement pstmtDecrement = conn.prepareStatement(decrementCountSql);
                 PreparedStatement pstmtIncrement = conn.prepareStatement(incrementCountSql);
                 PreparedStatement pstmtCheck = conn.prepareStatement(checkOtherProbationsSql);
                 PreparedStatement pstmtActive = conn.prepareStatement(updateStudentActiveSql)) {

                // 1. Update the report dates and reason
                pstmtUpdateReport.setDate(1, Date.valueOf(report.getStartDate()));
                pstmtUpdateReport.setDate(2, Date.valueOf(report.getEndDate()));
                pstmtUpdateReport.setString(3, report.getReason());
                pstmtUpdateReport.setString(4, report.getProbationId());
                pstmtUpdateReport.executeUpdate();

                // 2. Handle removed students
                for (String regNo : removedStudents) {
                    // Delete relation
                    pstmtDeleteStudent.setString(1, report.getProbationId());
                    pstmtDeleteStudent.setString(2, regNo);
                    pstmtDeleteStudent.executeUpdate();

                    // Decrement probation count
                    pstmtDecrement.setString(1, regNo);
                    pstmtDecrement.executeUpdate();

                    // Reset status to ACTIVE if no other reports exist
                    pstmtCheck.setString(1, regNo);
                    try (ResultSet rs = pstmtCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            pstmtActive.setString(1, regNo);
                            pstmtActive.executeUpdate();
                        }
                    }
                }

                // 3. Handle added students
                for (String regNo : addedStudents) {
                    // Insert relation
                    pstmtInsertStudent.setString(1, report.getProbationId());
                    pstmtInsertStudent.setString(2, regNo);
                    pstmtInsertStudent.executeUpdate();

                    // Set status to PROBATION and increment count
                    pstmtIncrement.setString(1, regNo);
                    pstmtIncrement.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataIntegrityException("Error updating probation report: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
}
