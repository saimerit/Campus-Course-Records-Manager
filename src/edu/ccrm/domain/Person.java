package edu.ccrm.domain;

public abstract class Person {
    private int id;
    private Name fullName;
    private String email;

    public Person(int id, Name fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
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

    public abstract String getProfile();

    public abstract String toCsvString();
}
