package com.servicedesk.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private final String id;
    private String name;
    private String email;
    private Role role;
    private String department;
    private boolean active;
    private final LocalDateTime createdAt;

    public User(String id, String name, String email, Role role, String department) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.role = Objects.requireNonNull(role, "Role must not be null");
        this.department = Objects.requireNonNull(department, "Department must not be null");
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "Email cannot be null");
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = Objects.requireNonNull(department, "Department cannot be null");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", department='" + department + '\'' +
                ", active=" + active +
                '}';
    }
}