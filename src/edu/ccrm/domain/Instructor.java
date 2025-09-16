package edu.ccrm.domain;

public class Instructor extends Person {
    private String department;

    public Instructor(int id, Name fullName, String email, String department) {
        super(id, fullName, email);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String getProfile() {
        return String.format("Instructor | ID: %d, Name: %s, Email: %s, Department: %s",
                getId(), getFullName(), getEmail(), department);
    }
    
    @Override
    public String toCsvString() {
        return String.join(",",
                String.valueOf(getId()),
                getFullName().getFirstName(),
                getFullName().getLastName(),
                getEmail(),
                getDepartment()
        );
    }
}
