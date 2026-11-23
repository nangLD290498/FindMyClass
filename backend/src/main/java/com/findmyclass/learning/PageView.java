package com.findmyclass.learning;

import jakarta.persistence.*;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "page_views")
public class PageView {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JdbcTypeCode(SqlTypes.NUMERIC)
  Long id;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(name = "teacher_id", nullable = false)
  Long teacherId;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(name = "course_id")
  Long courseId;

  @Column(nullable = false, length = 80)
  String source;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  protected PageView() {}
}
