package edu.ccrm.domain;

import java.time.LocalDate;

public class Instructor extends Person {
    private String department;
    private String fId;
    private String cabinNo;

    public Instructor(String fId, Name fullName, String email, String department, LocalDate dob, String phone, String cabinNo) {
        super(0, fullName, email, dob, phone); 
        this.fId = fId;
        this.department = department;
        this.cabinNo = cabinNo;
    }

    public String getFiD() {
        return fId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCabinNo() {
        return cabinNo;
    }

    public void setCabinNo(String cabinNo) {
        this.cabinNo = cabinNo;
    }

    @Override
    public String getProfile() {
        return String.format("Instructor | FiD: %s, Name: %s, Email: %s, DOB: %s, Phone: %s, Dept: %s, Cabin: %s",
                fId, getFullName(), getEmail(), getDob(), getPhone(), department, cabinNo);
    }
    
    @Override
    public String toCsvString() {
        return String.join(",",
                getFiD(),
                getFullName().getFirstName(),
                getFullName().getLastName(),
                getEmail(),
                getDepartment(),
                getDob() != null ? getDob().toString() : "",
                getPhone() != null ? getPhone() : "",
                getCabinNo() != null ? getCabinNo() : ""
        );
    }
}