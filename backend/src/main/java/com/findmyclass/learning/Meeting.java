package com.findmyclass.learning;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "course_meetings")
public class Meeting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JdbcTypeCode(SqlTypes.NUMERIC)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  Course course;

  @JdbcTypeCode(SqlTypes.NUMERIC)
  @Column(nullable = false)
  int weekday;

  @Column(name = "start_time", nullable = false, length = 5)
  String startTime;

  @Column(name = "end_time", nullable = false, length = 5)
  String endTime;

  protected Meeting() {}

  Meeting(Course c, int day, String start, String end) {
    course = c;
    weekday = day;
    startTime = start;
    endTime = end;
  }
}
