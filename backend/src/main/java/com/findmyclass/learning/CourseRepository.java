package com.findmyclass.learning;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourseRepository
    extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Course c where c.id=:id")
  Optional<Course> lock(@Param("id") Long id);

  long countByTeacherIdAndStatusNot(Long teacherId, Course.Status status);

  java.util.List<Course> findByTeacherIdAndStatusOrderByCreatedAtDesc(
      Long teacherId, Course.Status status);
}
