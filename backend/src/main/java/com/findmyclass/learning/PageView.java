package com.findmyclass.learning;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "page_views")
public class PageView {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "teacher_id", nullable = false)
  Long teacherId;

  @Column(name = "course_id")
  Long courseId;

  @Column(nullable = false, length = 80)
  String source;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  protected PageView() {}
}
