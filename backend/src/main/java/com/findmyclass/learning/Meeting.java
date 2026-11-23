package com.findmyclass.learning;

import jakarta.persistence.*;

@Entity
@Table(name = "course_meetings")
public class Meeting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  Course course;

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
