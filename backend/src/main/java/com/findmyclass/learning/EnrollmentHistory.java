package com.findmyclass.learning;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "enrollment_history")
public class EnrollmentHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "enrollment_id", nullable = false)
  Long enrollmentId;

  @Column(name = "actor_id", nullable = false)
  Long actorId;

  @Column(nullable = false, length = 16)
  String status;

  @Column(length = 2000)
  String note;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  protected EnrollmentHistory() {}
}
