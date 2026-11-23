package com.findmyclass.learning;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

  long countByCourseIdAndStatusIn(Long courseId, Collection<Enrollment.Status> statuses);

  List<Enrollment> findByCourseTeacherIdOrderByCreatedAtDesc(Long teacherId);

  List<Enrollment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
