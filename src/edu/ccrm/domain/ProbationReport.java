package edu.ccrm.domain;

import java.time.LocalDate;
import java.util.List;

public class ProbationReport {
    private final String probationId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private final List<String> studentRegNos;

    public ProbationReport(String probationId, LocalDate startDate, LocalDate endDate, String reason, List<String> studentRegNos) {
        this.probationId = probationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.studentRegNos = studentRegNos;
    }

    public String getProbationId() {
        return probationId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getStudentRegNos() {
        return studentRegNos;
    }
}
