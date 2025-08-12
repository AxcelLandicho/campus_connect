package com.campusconnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "app_users")
public class User {
    @Id
    @Column(unique = true, nullable = false)
    private String email;

    private String name;
    private String password;
    private String role;
    @Column(nullable = true)
    private String course;
    private String profilePicUrl;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public User() {}

    public User(String email, String name, String password, String role, String course) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role;
        this.course = course;
    }
}