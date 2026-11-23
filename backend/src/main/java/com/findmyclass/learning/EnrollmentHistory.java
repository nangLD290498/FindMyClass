package com.findmyclass.learning;

import jakarta.persistence.*;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "enrollment_history")
public class EnrollmentHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JdbcTypeCode(SqlTypes.NUMERIC)
  Long id;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(name = "enrollment_id", nullable = false)
  Long enrollmentId;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(name = "actor_id", nullable = false)
  Long actorId;

  @Column(nullable = false, length = 16)
  String status;

  @Lob String note;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  protected EnrollmentHistory() {}
}
