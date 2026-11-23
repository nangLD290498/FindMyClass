package com.findmyclass.learning;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageViewRepository extends JpaRepository<PageView, Long> {
  List<PageView> findByTeacherIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long teacherId, LocalDateTime from, LocalDateTime to);
}
