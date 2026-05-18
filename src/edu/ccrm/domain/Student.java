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

    public Student(int id, String regNo, Name fullName, String email, LocalDate dob, String phone) {
        super(id, fullName, email, dob, phone);
        this.regNo = regNo;
        this.status = Status.ACTIVE;
        this.registrationDate = LocalDate.now();
    }
    
    public Student(int id, String regNo, Name fullName, String email, Status status, LocalDate registrationDate, LocalDate dob, String phone) {
        super(id, fullName, email, dob, phone);
        this.regNo = regNo;
        this.status = status;
        this.registrationDate = registrationDate;
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

    @Override
    public String getProfile() {
        return String.format("Student | ID: %d, RegNo: %s, Name: %s, Email: %s, DOB: %s, Phone: %s, Status: %s, Registered On: %s",
                getId(), regNo, getFullName(), getEmail(), getDob(), getPhone(), status, registrationDate);
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
                getPhone() != null ? getPhone() : ""
        );
    }
}