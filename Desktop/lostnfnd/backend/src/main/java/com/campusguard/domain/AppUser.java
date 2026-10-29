package com.campusguard.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_student_id", columnList = "studentId")
})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String studentId;
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.Role role = Enums.Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.AccountStatus accountStatus = Enums.AccountStatus.PENDING_APPROVAL;

    private boolean collegeEmailFormat;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Enums.Role getRole() { return role; }
    public void setRole(Enums.Role role) { this.role = role; }
    public Enums.AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(Enums.AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public boolean isCollegeEmailFormat() { return collegeEmailFormat; }
    public void setCollegeEmailFormat(boolean collegeEmailFormat) { this.collegeEmailFormat = collegeEmailFormat; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
