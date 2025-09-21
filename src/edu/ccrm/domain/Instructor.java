package edu.ccrm.domain;

public class Instructor extends Person {
    private String department;
    private String fId;

    public Instructor(String fId, Name fullName, String email, String department) {
        super(0, fullName, email); 
        this.fId = fId;
        this.department = department;
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

    @Override
    public String getProfile() {
        return String.format("Instructor | FiD: %s, Name: %s, Email: %s, Department: %s",
                fId, getFullName(), getEmail(), department);
    }
    
    @Override
    public String toCsvString() {
        return String.join(",",
                getFiD(),
                getFullName().getFirstName(),
                getFullName().getLastName(),
                getEmail(),
                getDepartment()
        );
    }
}