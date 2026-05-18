package edu.ccrm.domain;

import java.time.LocalDate;

public abstract class Person {
    private int id;
    private Name fullName;
    private String email;
    private LocalDate dob;
    private String phone;

    public Person(int id, Name fullName, String email, LocalDate dob, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.dob = dob;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public Name getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDate getDob() {
        return dob;
    }
    
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public abstract String getProfile();

    public abstract String toCsvString();
}
