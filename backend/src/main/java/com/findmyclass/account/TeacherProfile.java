package com.findmyclass.account;
import jakarta.persistence.*;
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {
    @Id @Column(name = "user_id") private Long userId;
    @Column(nullable = false, length = 2000) private String bio;
    @Column(nullable = false, length = 500) private String subjects;
    public String getBio() { return bio; }
    public String getSubjects() { return subjects; }
    protected TeacherProfile() {}
    public TeacherProfile(Long userId, String bio, String subjects) {
        this.userId = userId; this.bio = bio; this.subjects = subjects;
    }
}
