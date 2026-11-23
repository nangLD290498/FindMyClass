package com.findmyclass;

import static org.assertj.core.api.Assertions.*;

import com.findmyclass.account.*;
import com.findmyclass.learning.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.free-course-limit=1")
@ActiveProfiles("test")
class PlanLimitTest {
  @Autowired AccountService accounts;
  @Autowired LearningService service;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setup() {
    clean();
    accounts.signIn("limited", "limited@example.com", "Teacher");
    accounts.chooseRole("limited", Role.TEACHER);
    accounts.completeProfile("limited", "Bio", "Math");
  }

  @AfterEach
  void clean() {
    for (String t :
        List.of(
            "enrollment_history",
            "enrollments",
            "page_views",
            "course_meetings",
            "courses",
            "teacher_profiles",
            "app_users")) jdbc.update("delete from " + t);
  }

  LearningService.CourseInput input() {
    return new LearningService.CourseInput(
        "Math",
        "Description",
        "Math",
        "10",
        "Hanoi",
        "School",
        Course.Mode.OFFLINE,
        BigDecimal.ZERO,
        2,
        LocalDate.of(2030, 1, 1),
        LocalDate.of(2030, 6, 1),
        List.of(new LearningService.MeetingInput(1, "18:00", "19:00")));
  }

  @Test
  void concurrentCreationCannotBypassConfiguredLimitAndArchivingReleasesIt() throws Exception {
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var tasks = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++)
        tasks.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    service.save("limited", null, input());
                    return 201;
                  } catch (org.springframework.web.server.ResponseStatusException ex) {
                    return ex.getStatusCode().value();
                  }
                }));
      gate.countDown();
      assertThat(
              List.of(
                  tasks.get(0).get(15, TimeUnit.SECONDS), tasks.get(1).get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(service.plan("limited").coursesUsed()).isEqualTo(1);
    long id = service.ownCourses("limited").get(0).id();
    service.publish("limited", id, Course.Status.ARCHIVED);
    assertThat(service.plan("limited").coursesUsed()).isZero();
    assertThat(service.save("limited", null, input()).status()).isEqualTo(Course.Status.DRAFT);
  }
}
