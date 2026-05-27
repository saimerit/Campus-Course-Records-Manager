package edu.ccrm.domain;

import java.time.LocalDate;

public class Student extends Person {

    public enum Status {
        ACTIVE,
        INACTIVE,
        GRADUATED,
        PROBATION
    }

    private String regNo;
    private Status status;
    private final LocalDate registrationDate;
    private Double cgpa; // Can be null if not computed yet
    private int probationCount = 0;
    private int gradedCredits = 0;

    public Student(int id, String regNo, Name fullName, String email, LocalDate dob, String phone) {
        super(id, fullName, email, dob, phone);
        this.regNo = regNo;
        this.status = Status.ACTIVE;
        this.registrationDate = LocalDate.now();
        this.probationCount = 0;
    }
    
    public Student(int id, String regNo, Name fullName, String email, Status status, LocalDate registrationDate, LocalDate dob, String phone) {
        super(id, fullName, email, dob, phone);
        this.regNo = regNo;
        this.status = status;
        this.registrationDate = registrationDate;
        this.probationCount = 0;
    }

    public String getRegNo() {
        return regNo;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public int getProbationCount() {
        return probationCount;
    }

    public void setProbationCount(int probationCount) {
        this.probationCount = probationCount;
    }

    public int getGradedCredits() {
        return gradedCredits;
    }

    public void setGradedCredits(int gradedCredits) {
        this.gradedCredits = gradedCredits;
    }

    @Override
    public String getProfile() {
        return String.format("Student | ID: %d, RegNo: %s, Name: %s, Email: %s, DOB: %s, Phone: %s, Status: %s, Registered On: %s, CGPA: %s, Probation Count: %d",
                getId(), regNo, getFullName(), getEmail(), getDob(), getPhone(), status, registrationDate, cgpa != null ? String.format("%.2f", cgpa) : "N/A", probationCount);
    }
    
    @Override
    public String toCsvString() {
        return String.join(",",
                String.valueOf(getId()),
                getRegNo(),
                getFullName().getFirstName(),
                getFullName().getLastName(),
                getEmail(),
                getStatus().name(),
                getRegistrationDate().toString(),
                getDob() != null ? getDob().toString() : "",
                getPhone() != null ? getPhone() : "",
                String.valueOf(getProbationCount())
        );
    }
}