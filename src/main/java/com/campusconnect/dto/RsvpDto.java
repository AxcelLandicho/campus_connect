package com.campusconnect.dto;

public class RsvpDto {
    private Long id;
    private String studentEmail;
    private boolean attended;
    private boolean present;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
