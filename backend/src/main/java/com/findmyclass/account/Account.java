package com.findmyclass.account;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name = "app_users")
public class Account {
    @Id @JdbcTypeCode(SqlTypes.NUMERIC) @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "google_subject", nullable = false, unique = true, length = 255)
    private String googleSubject;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    @Enumerated(EnumType.STRING) @Column(length = 16)
    private Role role;
    protected Account() {}
    public Account(String googleSubject, String email, String displayName) {
        this.googleSubject = googleSubject;
        updateIdentity(email, displayName);
    }
    public void updateIdentity(String email, String displayName) { this.email = email; this.displayName = displayName; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public void chooseRole(Role role) { this.role = role; }
}
