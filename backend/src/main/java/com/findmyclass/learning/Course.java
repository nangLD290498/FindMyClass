package com.findmyclass.learning;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "courses")
public class Course {
  public enum Status {
    DRAFT,
    PUBLISHED,
    HIDDEN,
    ARCHIVED
  }

  public enum Mode {
    ONLINE,
    OFFLINE
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "teacher_id", nullable = false)
  Long teacherId;

  @Column(nullable = false, length = 160)
  String title;

  @Column(nullable = false, length = 4000)
  String description;

  @Column(nullable = false, length = 100)
  String subject;

  @Column(name = "level_name", nullable = false, length = 100)
  String level;

  @Column(nullable = false, length = 160)
  String area;

  @Column(nullable = false, length = 500)
  String location;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "delivery_mode", nullable = false, length = 16)
  Mode mode;

  @Column(nullable = false, precision = 14, scale = 2)
  BigDecimal tuition;

  @Column(nullable = false)
  int capacity;

  @Column(name = "start_date", nullable = false)
  LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 16)
  Status status = Status.DRAFT;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("weekday ASC, startTime ASC")
  List<Meeting> meetings = new ArrayList<>();

  protected Course() {}
}
