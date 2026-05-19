package edu.ccrm.service;

import edu.ccrm.domain.CourseCode;
import edu.ccrm.domain.Grade;
import edu.ccrm.io.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsService {

    public Map<String, Integer> getGradeDistributionForCourse(CourseCode courseCode) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (Grade grade : Grade.values()) {
            if (grade != Grade.NA) {
                distribution.put(grade.name(), 0);
            }
        }

        String sql = "SELECT grade, COUNT(*) FROM enrollments WHERE course_code = ? AND grade IS NOT NULL AND grade <> 'NA' GROUP BY grade";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode.getCode());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    distribution.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching course grade distribution: " + e.getMessage());
        }
        return distribution;
    }

    public Map<String, Integer> getGradeDistributionForDepartment(String department) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (Grade grade : Grade.values()) {
            if (grade != Grade.NA) {
                distribution.put(grade.name(), 0);
            }
        }

        String sql = "SELECT e.grade, COUNT(*) FROM enrollments e "
                   + "JOIN courses c ON e.course_code = c.code "
                   + "WHERE c.department = ? AND e.grade IS NOT NULL AND e.grade <> 'NA' "
                   + "GROUP BY e.grade";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, department);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    distribution.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching department grade distribution: " + e.getMessage());
        }
        return distribution;
    }

    public List<InstructorAnalytics> getInstructorAnalytics() {
        List<InstructorAnalytics> list = new ArrayList<>();
        String sql = "SELECT i.FiD, i.first_name, i.last_name, i.department, "
                   + "SUM(CASE e.grade "
                   + "  WHEN 'S' THEN 10.0 * c.credits "
                   + "  WHEN 'A' THEN 9.0 * c.credits "
                   + "  WHEN 'B' THEN 8.0 * c.credits "
                   + "  WHEN 'C' THEN 7.0 * c.credits "
                   + "  WHEN 'D' THEN 6.0 * c.credits "
                   + "  WHEN 'E' THEN 5.0 * c.credits "
                   + "  WHEN 'F' THEN 0.0 * c.credits "
                   + "  ELSE 0.0 END) as total_weighted_points, "
                   + "SUM(CASE WHEN e.grade IN ('S','A','B','C','D','E','F') THEN c.credits ELSE 0 END) as total_credits, "
                   + "COUNT(CASE WHEN e.grade IN ('S','A','B','C','D','E','F') THEN 1 END) as total_graded "
                   + "FROM instructors i "
                   + "LEFT JOIN courses c ON i.FiD = c.instructor_id "
                   + "LEFT JOIN enrollments e ON c.code = e.course_code "
                   + "GROUP BY i.FiD, i.first_name, i.last_name, i.department";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String fid = rs.getString("FiD");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String dept = rs.getString("department");
                double weightedPoints = rs.getDouble("total_weighted_points");
                int totalCredits = rs.getInt("total_credits");
                int gradedCount = rs.getInt("total_graded");

                double avgGrade = totalCredits == 0 ? 0.0 : weightedPoints / totalCredits;
                list.add(new InstructorAnalytics(fid, name, dept, avgGrade, gradedCount));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching instructor analytics: " + e.getMessage());
        }
        return list;
    }

    public List<CoursePopularityAnalytics> getCoursePopularity() {
        List<CoursePopularityAnalytics> list = new ArrayList<>();
        String sql = "SELECT c.code, c.title, c.department, "
                   + "(SELECT COUNT(*) FROM enrollments e WHERE e.course_code = c.code) as active_count, "
                   + "(SELECT COUNT(*) FROM dropped_enrollments d WHERE d.course_code = c.code) as dropped_count "
                   + "FROM courses c";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("code");
                String title = rs.getString("title");
                String dept = rs.getString("department");
                int active = rs.getInt("active_count");
                int dropped = rs.getInt("dropped_count");

                double dropoutRate = (active + dropped) == 0 ? 0.0 : (dropped / (double)(active + dropped)) * 100.0;
                list.add(new CoursePopularityAnalytics(code, title, dept, active, dropped, dropoutRate));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching course popularity metrics: " + e.getMessage());
        }
        return list;
    }

    public List<String> getAllDepartments() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT department FROM courses WHERE department IS NOT NULL ORDER BY department";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching departments: " + e.getMessage());
        }
        return list;
    }

    public static class InstructorAnalytics {
        private final String fid;
        private final String name;
        private final String department;
        private final double averageGrade;
        private final int gradedStudents;

        public InstructorAnalytics(String fid, String name, String department, double averageGrade, int gradedStudents) {
            this.fid = fid;
            this.name = name;
            this.department = department;
            this.averageGrade = averageGrade;
            this.gradedStudents = gradedStudents;
        }

        public String getFid() { return fid; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getAverageGrade() { return averageGrade; }
        public int getGradedStudents() { return gradedStudents; }
    }

    public static class CoursePopularityAnalytics {
        private final String courseCode;
        private final String title;
        private final String department;
        private final int activeCount;
        private final int droppedCount;
        private final double dropoutRate;

        public CoursePopularityAnalytics(String courseCode, String title, String department, int activeCount, int droppedCount, double dropoutRate) {
            this.courseCode = courseCode;
            this.title = title;
            this.department = department;
            this.activeCount = activeCount;
            this.droppedCount = droppedCount;
            this.dropoutRate = dropoutRate;
        }

        public String getCourseCode() { return courseCode; }
        public String getTitle() { return title; }
        public String getDepartment() { return department; }
        public int getActiveCount() { return activeCount; }
        public int getDroppedCount() { return droppedCount; }
        public double getDropoutRate() { return dropoutRate; }
    }
}
