package com.findmyclass.learning;

import jakarta.persistence.*;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "enrollments")
public class Enrollment {
  public enum Status {
    NEW,
    CONTACTED,
    CONFIRMED,
    ATTENDING,
    CANCELLED,
    REJECTED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JdbcTypeCode(SqlTypes.NUMERIC)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  Course course;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(name = "student_id", nullable = false)
  Long studentId;

  @Column(nullable = false, length = 30)
  String phone;

  @Column(length = 1000)
  String message;

  @Column(nullable = false, length = 80)
  String source;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  Status status = Status.NEW;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  protected Enrollment() {}
}
