package com.findmyclass.learning;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<EnrollmentHistory, Long> {
  List<EnrollmentHistory> findByEnrollmentIdOrderByCreatedAtAscIdAsc(Long id);
}
