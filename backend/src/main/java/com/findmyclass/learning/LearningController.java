package com.findmyclass.learning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class LearningController {
  private final LearningService service;

  public LearningController(LearningService service) {
    this.service = service;
  }

  @GetMapping("/api/public/courses")
  public LearningService.PageResult<LearningService.CourseView> search(
      @RequestParam(required = false) @Size(max = 160) String q,
      @RequestParam(required = false) @Size(max = 100) String subject,
      @RequestParam(required = false) @Size(max = 160) String area,
      @RequestParam(required = false) Course.Mode mode,
      @RequestParam(required = false) BigDecimal min,
      @RequestParam(required = false) BigDecimal max,
      @RequestParam(required = false) Integer weekday,
      @RequestParam(defaultValue = "0") int page) {
    return service.search(q, subject, area, mode, min, max, weekday, page);
  }

  @GetMapping("/api/public/courses/{id}")
  public LearningService.CourseView detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @GetMapping("/api/public/teachers/{id}")
  public LearningService.TeacherView teacher(@PathVariable Long id) {
    return service.publicTeacher(id);
  }

  public record ViewInput(Long teacherId, Long courseId, @Size(max = 80) String source) {}

  @PostMapping("/api/public/views")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void view(@AuthenticationPrincipal OidcUser user, @Valid @RequestBody ViewInput input) {
    if (input.teacherId() == null && input.courseId() == null)
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
    service.track(
        input.teacherId(),
        input.courseId(),
        input.source(),
        user == null ? null : user.getSubject());
  }

  @GetMapping("/api/teacher/courses")
  public List<LearningService.CourseView> own(@AuthenticationPrincipal OidcUser u) {
    return service.ownCourses(u.getSubject());
  }

  @GetMapping("/api/teacher/courses/{id}")
  public LearningService.CourseView ownDetail(
      @AuthenticationPrincipal OidcUser u, @PathVariable Long id) {
    return service.ownDetail(u.getSubject(), id);
  }

  @PostMapping("/api/teacher/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningService.CourseView create(
      @AuthenticationPrincipal OidcUser u, @Valid @RequestBody LearningService.CourseInput input) {
    return service.save(u.getSubject(), null, input);
  }

  @PutMapping("/api/teacher/courses/{id}")
  public LearningService.CourseView edit(
      @AuthenticationPrincipal OidcUser u,
      @PathVariable Long id,
      @Valid @RequestBody LearningService.CourseInput input) {
    return service.save(u.getSubject(), id, input);
  }

  public record StatusInput(@NotNull Course.Status status) {}

  @PutMapping("/api/teacher/courses/{id}/status")
  public LearningService.CourseView status(
      @AuthenticationPrincipal OidcUser u,
      @PathVariable Long id,
      @Valid @RequestBody StatusInput input) {
    return service.publish(u.getSubject(), id, input.status());
  }

  @PostMapping("/api/student/courses/{id}/applications")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningService.EnrollmentView apply(
      @AuthenticationPrincipal OidcUser u,
      @PathVariable Long id,
      @Valid @RequestBody LearningService.ApplyInput input) {
    return service.apply(u.getSubject(), id, input);
  }

  @GetMapping("/api/student/applications")
  public List<LearningService.EnrollmentView> studentApplications(
      @AuthenticationPrincipal OidcUser u) {
    return service.studentApplications(u.getSubject());
  }

  @GetMapping("/api/teacher/applications")
  public List<LearningService.EnrollmentView> teacherApplications(
      @AuthenticationPrincipal OidcUser u) {
    return service.teacherApplications(u.getSubject());
  }

  @PutMapping("/api/teacher/applications/{id}")
  public LearningService.EnrollmentView update(
      @AuthenticationPrincipal OidcUser u,
      @PathVariable Long id,
      @Valid @RequestBody LearningService.UpdateInput input) {
    return service.update(u.getSubject(), id, input, true);
  }

  @PutMapping("/api/student/applications/{id}/cancel")
  public LearningService.EnrollmentView cancel(
      @AuthenticationPrincipal OidcUser u, @PathVariable Long id) {
    return service.update(
        u.getSubject(),
        id,
        new LearningService.UpdateInput(Enrollment.Status.CANCELLED, null),
        false);
  }

  @GetMapping("/api/teacher/analytics")
  public LearningService.Analytics analytics(
      @AuthenticationPrincipal OidcUser u,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return service.analytics(u.getSubject(), from, to);
  }

  @GetMapping("/api/teacher/plan")
  public LearningService.PlanView plan(@AuthenticationPrincipal OidcUser u) {
    return service.plan(u.getSubject());
  }
}
