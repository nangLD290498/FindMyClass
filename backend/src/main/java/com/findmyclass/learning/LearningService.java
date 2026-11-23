package com.findmyclass.learning;

import com.findmyclass.account.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LearningService {
  private static final List<Enrollment.Status> SEATED =
      List.of(Enrollment.Status.CONFIRMED, Enrollment.Status.ATTENDING);
  private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
  private final AccountRepository accounts;
  private final TeacherProfileRepository profiles;
  private final CourseRepository courses;
  private final EnrollmentRepository enrollments;
  private final HistoryRepository history;
  private final PageViewRepository views;
  private final int freeLimit;

  public LearningService(
      AccountRepository a,
      TeacherProfileRepository p,
      CourseRepository c,
      EnrollmentRepository e,
      HistoryRepository h,
      PageViewRepository v,
      @Value("${app.free-course-limit:-1}") int limit) {
    accounts = a;
    profiles = p;
    courses = c;
    enrollments = e;
    history = h;
    views = v;
    freeLimit = limit;
  }

  public record MeetingInput(
      @Min(1) @Max(7) int weekday,
      @NotNull @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String startTime,
      @NotNull @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String endTime) {}

  public record CourseInput(
      @NotBlank @Size(max = 160) String title,
      @NotBlank @Size(max = 4000) String description,
      @NotBlank @Size(max = 100) String subject,
      @NotBlank @Size(max = 100) String level,
      @NotBlank @Size(max = 160) String area,
      @NotBlank @Size(max = 500) String location,
      @NotNull Course.Mode mode,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal tuition,
      @Min(1) @Max(10000) int capacity,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      @NotEmpty @Size(max = 21) List<@NotNull @Valid MeetingInput> meetings) {}

  public record CourseView(
      Long id,
      Long teacherId,
      String teacherName,
      String title,
      String description,
      String subject,
      String level,
      String area,
      String location,
      Course.Mode mode,
      BigDecimal tuition,
      String pricingUnit,
      int capacity,
      long enrolled,
      LocalDate startDate,
      LocalDate endDate,
      Course.Status status,
      List<MeetingInput> meetings) {}

  public record PageResult<T>(List<T> content, long totalElements, int totalPages, int page) {}

  public record TeacherView(
      Long id, String name, String bio, String subjects, List<CourseView> courses) {}

  public record ApplyInput(
      @NotBlank @Pattern(regexp = "[+0-9() .-]{7,30}") String phone,
      @Size(max = 1000) String message,
      @Size(max = 80) String source,
      @AssertTrue boolean contactConsent) {}

  public record UpdateInput(@NotNull Enrollment.Status status, @Size(max = 2000) String note) {}

  public record HistoryView(
      String status, String note, String actorName, LocalDateTime createdAt) {}

  public record EnrollmentView(
      Long id,
      CourseView course,
      String studentName,
      String email,
      String phone,
      String message,
      String source,
      Enrollment.Status status,
      LocalDateTime createdAt,
      List<HistoryView> history) {}

  public record SourceStats(String source, long views, long applications, long confirmed) {}

  public record Analytics(
      long views,
      long applications,
      long confirmed,
      double conversionRate,
      List<SourceStats> sources) {}

  public record PlanView(
      String name, int courseLimit, long coursesUsed, boolean upgradesAvailable) {}

  private static ResponseStatusException error(HttpStatus s, String text) {
    return new ResponseStatusException(s, text);
  }

  private Account account(String sub) {
    return accounts
        .findByGoogleSubject(sub)
        .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại."));
  }

  private Account teacher(String sub) {
    Account a = account(sub);
    if (a.getRole() != Role.TEACHER || !profiles.existsById(a.getId()))
      throw error(HttpStatus.FORBIDDEN, "Cần hoàn thiện hồ sơ giáo viên.");
    return a;
  }

  private Account student(String sub) {
    Account a = account(sub);
    if (a.getRole() != Role.STUDENT)
      throw error(HttpStatus.FORBIDDEN, "Chức năng này dành cho học viên.");
    return a;
  }

  private Course owned(Long id, Long owner, boolean lock) {
    Course c =
        (lock ? courses.lock(id) : courses.findById(id))
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));
    c = (Course) org.hibernate.Hibernate.unproxy(c);
    if (!c.teacherId.equals(owner)) throw error(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học.");
    return c;
  }

  private Course published(Long id) {
    Course c =
        courses
            .findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));
    c = (Course) org.hibernate.Hibernate.unproxy(c);
    if (c.status != Course.Status.PUBLISHED)
      throw error(HttpStatus.NOT_FOUND, "Khóa học chưa công khai hoặc đã ẩn.");
    return c;
  }

  private long seated(Course c) {
    return enrollments.countByCourseIdAndStatusIn(c.id, SEATED);
  }

  private CourseView view(Course c) {
    c = (Course) org.hibernate.Hibernate.unproxy(c);
    String name = accounts.findById(c.teacherId).orElseThrow().getDisplayName();
    return new CourseView(
        c.id,
        c.teacherId,
        name,
        c.title,
        c.description,
        c.subject,
        c.level,
        c.area,
        c.location,
        c.mode,
        c.tuition,
        "COURSE",
        c.capacity,
        seated(c),
        c.startDate,
        c.endDate,
        c.status,
        c.meetings.stream().map(m -> new MeetingInput(m.weekday, m.startTime, m.endTime)).toList());
  }

  public CourseView detail(Long id) {
    return view(published(id));
  }

  public CourseView ownDetail(String sub, Long id) {
    return view(owned(id, teacher(sub).getId(), false));
  }

  public PageResult<CourseView> search(
      String q,
      String subject,
      String area,
      Course.Mode mode,
      BigDecimal min,
      BigDecimal max,
      Integer weekday,
      int page) {
    if (page < 0
        || (weekday != null && (weekday < 1 || weekday > 7))
        || (min != null && min.signum() < 0)
        || (max != null && max.signum() < 0)
        || (min != null && max != null && min.compareTo(max) > 0))
      throw error(HttpStatus.BAD_REQUEST, "Bộ lọc không hợp lệ.");
    Specification<Course> spec =
        (r, query, b) -> {
          var predicates = new ArrayList<Predicate>();
          predicates.add(b.equal(r.get("status"), Course.Status.PUBLISHED));
          if (q != null && !q.isBlank()) {
            String like = like(q);
            var teacherQuery = query.subquery(Long.class);
            var t = teacherQuery.from(Account.class);
            teacherQuery
                .select(t.get("id"))
                .where(b.like(b.lower(t.get("displayName")), like, '\\'));
            predicates.add(
                b.or(
                    b.like(b.lower(r.get("title")), like, '\\'),
                    b.like(b.lower(r.get("subject")), like, '\\'),
                    r.get("teacherId").in(teacherQuery)));
          }
          if (subject != null && !subject.isBlank())
            predicates.add(b.like(b.lower(r.get("subject")), like(subject), '\\'));
          if (area != null && !area.isBlank())
            predicates.add(b.like(b.lower(r.get("area")), like(area), '\\'));
          if (mode != null) predicates.add(b.equal(r.get("mode"), mode));
          if (min != null) predicates.add(b.ge(r.get("tuition"), min));
          if (max != null) predicates.add(b.le(r.get("tuition"), max));
          if (weekday != null) {
            var sq = query.subquery(Long.class);
            var m = sq.from(Meeting.class);
            sq.select(m.get("course").get("id")).where(b.equal(m.get("weekday"), weekday));
            predicates.add(r.get("id").in(sq));
          }
          return b.and(predicates.toArray(Predicate[]::new));
        };
    var result =
        courses.findAll(
            spec,
            PageRequest.of(page, 12, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    return new PageResult<>(
        result.getContent().stream().map(this::view).toList(),
        result.getTotalElements(),
        result.getTotalPages(),
        page);
  }

  private static String like(String s) {
    return "%"
        + s.strip()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }

  public TeacherView publicTeacher(Long id) {
    Account a =
        accounts
            .findById(id)
            .filter(x -> x.getRole() == Role.TEACHER)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên."));
    TeacherProfile p =
        profiles
            .findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Giáo viên chưa hoàn thiện hồ sơ."));
    return new TeacherView(
        id,
        a.getDisplayName(),
        p.getBio(),
        p.getSubjects(),
        courses.findByTeacherIdAndStatusOrderByCreatedAtDesc(id, Course.Status.PUBLISHED).stream()
            .map(this::view)
            .toList());
  }

  public List<CourseView> ownCourses(String sub) {
    Long id = teacher(sub).getId();
    return courses
        .findAll(
            (Specification<Course>) (r, q, b) -> b.equal(r.get("teacherId"), id),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        .stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  public CourseView save(String sub, Long id, CourseInput input) {
    Account a = teacher(sub);
    // The teacher row serializes course creation so configured plan limits cannot be bypassed
    // concurrently.
    accounts.lockBySubject(sub).orElseThrow();
    if (id == null
        && freeLimit >= 0
        && courses.countByTeacherIdAndStatusNot(a.getId(), Course.Status.ARCHIVED) >= freeLimit)
      throw error(HttpStatus.CONFLICT, "Đã đạt giới hạn khóa học của gói hiện tại.");
    Course c = id == null ? new Course() : owned(id, a.getId(), true);
    if (c.status == Course.Status.ARCHIVED)
      throw error(HttpStatus.CONFLICT, "Khóa học lưu trữ không thể chỉnh sửa.");
    if (input.endDate().isBefore(input.startDate()))
      throw error(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
    List<MeetingInput> ms = input.meetings();
    for (int i = 0; i < ms.size(); i++) {
      MeetingInput m = ms.get(i);
      if (m.startTime().compareTo(m.endTime()) >= 0)
        throw error(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu.");
      for (int j = 0; j < i; j++) {
        var n = ms.get(j);
        if (m.weekday() == n.weekday()
            && m.startTime().compareTo(n.endTime()) < 0
            && n.startTime().compareTo(m.endTime()) < 0)
          throw error(HttpStatus.BAD_REQUEST, "Các buổi học không được trùng giờ.");
      }
    }
    if (id != null && input.capacity() < seated(c))
      throw error(HttpStatus.CONFLICT, "Sĩ số không được thấp hơn số học viên đã nhận.");
    if (id != null
        && seated(c) > 0
        && (!c.startDate.equals(input.startDate())
            || !c.endDate.equals(input.endDate())
            || !c.location.equals(input.location().strip())
            || c.mode != input.mode()
            || c.tuition.compareTo(input.tuition()) != 0
            || !new HashSet<>(
                    c.meetings.stream()
                        .map(m -> new MeetingInput(m.weekday, m.startTime, m.endTime))
                        .toList())
                .equals(new HashSet<>(ms))))
      throw error(
          HttpStatus.CONFLICT,
          "Đã có học viên: chưa hỗ trợ đổi lịch, địa điểm, học phí hoặc hình thức học.");
    c.teacherId = a.getId();
    c.title = input.title().strip();
    c.description = input.description().strip();
    c.subject = input.subject().strip();
    c.level = input.level().strip();
    c.area = input.area().strip();
    c.location = input.location().strip();
    c.mode = input.mode();
    c.tuition = input.tuition();
    c.capacity = input.capacity();
    c.startDate = input.startDate();
    c.endDate = input.endDate();
    c.meetings.clear();
    ms.forEach(m -> c.meetings.add(new Meeting(c, m.weekday(), m.startTime(), m.endTime())));
    courses.saveAndFlush(c);
    return view(c);
  }

  @Transactional
  public CourseView publish(String sub, Long id, Course.Status status) {
    Course c = owned(id, teacher(sub).getId(), true);
    if (c.status == Course.Status.ARCHIVED || status == Course.Status.DRAFT)
      throw error(HttpStatus.CONFLICT, "Không thể chuyển sang trạng thái này.");
    if (status == Course.Status.PUBLISHED && c.endDate.isBefore(LocalDate.now(VIETNAM)))
      throw error(HttpStatus.CONFLICT, "Khóa học đã kết thúc.");
    c.status = status;
    return view(c);
  }

  private String source(String s) {
    return s != null && s.matches("[a-zA-Z0-9_.-]{1,80}") ? s.toLowerCase(Locale.ROOT) : "direct";
  }

  @Transactional
  public void track(Long teacherId, Long courseId, String rawSource, String sub) {
    if (courseId != null) teacherId = published(courseId).teacherId;
    else publicTeacher(teacherId);
    if (sub != null && account(sub).getId().equals(teacherId)) return;
    PageView v = new PageView();
    v.teacherId = teacherId;
    v.courseId = courseId;
    v.source = source(rawSource);
    views.save(v);
  }

  private void log(Enrollment e, Long actor, String note) {
    EnrollmentHistory h = new EnrollmentHistory();
    h.enrollmentId = e.id;
    h.actorId = actor;
    h.status = e.status.name();
    h.note = note == null || note.isBlank() ? null : note.strip();
    history.save(h);
  }

  @Transactional
  public EnrollmentView apply(String sub, Long courseId, ApplyInput input) {
    Account a = student(sub);
    Course c =
        courses
            .lock(courseId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));
    if (c.status != Course.Status.PUBLISHED || c.endDate.isBefore(LocalDate.now(VIETNAM)))
      throw error(HttpStatus.CONFLICT, "Khóa học không còn nhận đăng ký.");
    if (seated(c) >= c.capacity) throw error(HttpStatus.CONFLICT, "Lớp đã đủ chỗ.");
    if (enrollments.findByCourseIdAndStudentId(c.id, a.getId()).isPresent())
      throw error(HttpStatus.CONFLICT, "Bạn đã gửi đăng ký cho khóa học này.");
    Enrollment e = new Enrollment();
    e.course = c;
    e.studentId = a.getId();
    e.phone = input.phone().strip();
    e.message = input.message() == null ? null : input.message().strip();
    e.source = source(input.source());
    enrollments.saveAndFlush(e);
    log(e, a.getId(), "Học viên đồng ý chia sẻ thông tin liên hệ và gửi đăng ký.");
    return enrollmentView(e, false);
  }

  private EnrollmentView enrollmentView(Enrollment e, boolean teacher) {
    Account a = accounts.findById(e.studentId).orElseThrow();
    // CRM notes are teacher-private; students receive status history without notes or actor
    // identity.
    var hs =
        history.findByEnrollmentIdOrderByCreatedAtAscIdAsc(e.id).stream()
            .map(
                h ->
                    new HistoryView(
                        h.status,
                        teacher ? h.note : null,
                        teacher
                            ? accounts.findById(h.actorId).orElseThrow().getDisplayName()
                            : null,
                        h.createdAt))
            .toList();
    return new EnrollmentView(
        e.id,
        view(e.course),
        a.getDisplayName(),
        a.getEmail(),
        e.phone,
        e.message,
        e.source,
        e.status,
        e.createdAt,
        hs);
  }

  public List<EnrollmentView> teacherApplications(String sub) {
    return enrollments.findByCourseTeacherIdOrderByCreatedAtDesc(teacher(sub).getId()).stream()
        .map(e -> enrollmentView(e, true))
        .toList();
  }

  public List<EnrollmentView> studentApplications(String sub) {
    return enrollments.findByStudentIdOrderByCreatedAtDesc(student(sub).getId()).stream()
        .map(e -> enrollmentView(e, false))
        .toList();
  }

  @Transactional
  public EnrollmentView update(String sub, Long id, UpdateInput input, boolean asTeacher) {
    Account a = asTeacher ? teacher(sub) : student(sub);
    Enrollment existing =
        enrollments
            .findById(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký."));
    Course original = (Course) org.hibernate.Hibernate.unproxy(existing.course);
    if (asTeacher ? !original.teacherId.equals(a.getId()) : !existing.studentId.equals(a.getId()))
      throw error(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký.");
    Course c = (Course) org.hibernate.Hibernate.unproxy(courses.lock(original.id).orElseThrow());
    entityManager.refresh(c);
    // Reload after acquiring the common course lock; another transaction may have changed this
    // application.
    Enrollment e = enrollments.findById(id).orElseThrow();
    // Refresh is needed because the first ownership lookup may already have loaded the entity.
    entityManager.refresh(e);
    if (!asTeacher
        && (input.status() != Enrollment.Status.CANCELLED
            || e.status == Enrollment.Status.ATTENDING
            || e.status == Enrollment.Status.CANCELLED
            || e.status == Enrollment.Status.REJECTED))
      throw error(HttpStatus.CONFLICT, "Không thể hủy đăng ký ở trạng thái này.");
    if (asTeacher && e.status != input.status() && !transitions(e.status).contains(input.status()))
      throw error(HttpStatus.CONFLICT, "Chuyển trạng thái không hợp lệ.");
    if (!SEATED.contains(e.status) && SEATED.contains(input.status()) && seated(c) >= c.capacity)
      throw error(HttpStatus.CONFLICT, "Lớp đã đủ chỗ.");
    if (c.status == Course.Status.ARCHIVED
        && e.status != input.status()
        && input.status() != Enrollment.Status.CANCELLED
        && input.status() != Enrollment.Status.REJECTED)
      throw error(HttpStatus.CONFLICT, "Khóa học đã lưu trữ; chỉ có thể đóng đăng ký.");
    if (e.status == input.status() && (input.note() == null || input.note().isBlank()))
      return enrollmentView(e, asTeacher);
    e.status = input.status();
    log(e, a.getId(), asTeacher ? input.note() : "Học viên hủy đăng ký.");
    return enrollmentView(e, asTeacher);
  }

  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

  private Set<Enrollment.Status> transitions(Enrollment.Status s) {
    return switch (s) {
      case NEW ->
          Set.of(
              Enrollment.Status.CONTACTED,
              Enrollment.Status.CONFIRMED,
              Enrollment.Status.REJECTED,
              Enrollment.Status.CANCELLED);
      case CONTACTED ->
          Set.of(
              Enrollment.Status.CONFIRMED, Enrollment.Status.REJECTED, Enrollment.Status.CANCELLED);
      case CONFIRMED -> Set.of(Enrollment.Status.ATTENDING, Enrollment.Status.CANCELLED);
      case ATTENDING -> Set.of(Enrollment.Status.CANCELLED);
      default -> Set.of();
    };
  }

  public Analytics analytics(String sub, LocalDate from, LocalDate to) {
    Long id = teacher(sub).getId();
    if (from == null) from = LocalDate.now(VIETNAM).minusDays(29);
    if (to == null) to = LocalDate.now(VIETNAM);
    if (to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365)
      throw error(HttpStatus.BAD_REQUEST, "Khoảng thống kê tối đa 366 ngày.");
    LocalDateTime
        start = LocalDateTime.ofInstant(from.atStartOfDay(VIETNAM).toInstant(), ZoneOffset.UTC),
        end =
            LocalDateTime.ofInstant(
                to.plusDays(1).atStartOfDay(VIETNAM).toInstant(), ZoneOffset.UTC);
    var totals = new TreeMap<String, long[]>();
    views
        .findByTeacherIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(id, start, end)
        .forEach(v -> totals.computeIfAbsent(v.source, k -> new long[3])[0]++);
    enrollments.findByCourseTeacherIdOrderByCreatedAtDesc(id).stream()
        .filter(e -> !e.createdAt.isBefore(start) && e.createdAt.isBefore(end))
        .forEach(
            e -> {
              var counts = totals.computeIfAbsent(e.source, k -> new long[3]);
              counts[1]++;
              if (SEATED.contains(e.status)) counts[2]++;
            });
    var rows =
        totals.entrySet().stream()
            .map(
                x -> new SourceStats(x.getKey(), x.getValue()[0], x.getValue()[1], x.getValue()[2]))
            .toList();
    long v = rows.stream().mapToLong(SourceStats::views).sum(),
        a = rows.stream().mapToLong(SourceStats::applications).sum(),
        c = rows.stream().mapToLong(SourceStats::confirmed).sum();
    return new Analytics(v, a, c, a == 0 ? 0 : 100.0 * c / a, rows);
  }

  public PlanView plan(String sub) {
    Long id = teacher(sub).getId();
    return new PlanView(
        "Free", freeLimit, courses.countByTeacherIdAndStatusNot(id, Course.Status.ARCHIVED), false);
  }
}
