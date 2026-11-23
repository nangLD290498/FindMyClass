package com.findmyclass;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.findmyclass.account.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearningFlowTest {
  @Autowired MockMvc mvc;
  @Autowired AccountService accounts;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  org.springframework.test.web.servlet.request.RequestPostProcessor login(String sub) {
    return oidcLogin().idToken(t -> t.subject(sub));
  }

  @BeforeEach
  void setup() {
    clean();
    for (String s : List.of("teacher", "other", "student", "student2")) {
      accounts.signIn(s, s + "@example.com", s);
      accounts.chooseRole(s, s.contains("student") ? Role.STUDENT : Role.TEACHER);
      if (!s.contains("student")) accounts.completeProfile(s, "I teach mathematics.", "Toán");
    }
  }

  @AfterEach
  void clean() {
    for (String table :
        List.of(
            "enrollment_history",
            "enrollments",
            "page_views",
            "course_meetings",
            "courses",
            "teacher_profiles",
            "app_users")) jdbc.update("delete from " + table);
  }

  Map<String, Object> input(int capacity) {
    return new LinkedHashMap<>(
        Map.ofEntries(
            Map.entry("title", "Toán lớp 10"),
            Map.entry("description", "Học đại số và hình học."),
            Map.entry("subject", "Toán"),
            Map.entry("level", "Lớp 10"),
            Map.entry("area", "Quận 3"),
            Map.entry("location", "Trung tâm A"),
            Map.entry("mode", "OFFLINE"),
            Map.entry("tuition", 1000000),
            Map.entry("capacity", capacity),
            Map.entry("startDate", "2030-01-01"),
            Map.entry("endDate", "2030-06-01"),
            Map.entry(
                "meetings",
                List.of(Map.of("weekday", 1, "startTime", "18:00", "endTime", "19:30")))));
  }

  long create(int capacity) throws Exception {
    return json.readTree(
            mvc.perform(
                    post("/api/teacher/courses")
                        .with(login("teacher"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(json.writeValueAsString(input(capacity))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asLong();
  }

  void publish(long id) throws Exception {
    mvc.perform(
            put("/api/teacher/courses/" + id + "/status")
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"PUBLISHED\"}"))
        .andExpect(status().isOk());
  }

  long apply(String who, long id) throws Exception {
    return json.readTree(
            mvc.perform(
                    post("/api/student/courses/" + id + "/applications")
                        .with(login(who))
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            "{\"phone\":\"0901234567\",\"message\":\"Please contact"
                                + " me\",\"source\":\"facebook\",\"contactConsent\":true}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asLong();
  }

  int confirm(long application) throws Exception {
    return mvc.perform(
            put("/api/teacher/applications/" + application)
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"CONFIRMED\",\"note\":\"Private follow-up\"}"))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void publicSearchFiltersAndTeacherPagesExposeOnlyPublishedCourses() throws Exception {
    long id = create(2);
    long tid = accounts.current("teacher").id();
    mvc.perform(get("/api/public/courses/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/courses")).andExpect(jsonPath("$.totalElements").value(0));
    publish(id);
    mvc.perform(
            get("/api/public/courses")
                .param("q", "teacher")
                .param("subject", "Toán")
                .param("area", "Quận")
                .param("weekday", "1")
                .param("mode", "OFFLINE")
                .param("min", "900000")
                .param("max", "1100000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/public/courses").param("weekday", "2"))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/public/courses").param("q", "%_"))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/public/teachers/" + tid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courses.length()").value(1))
        .andExpect(jsonPath("$.email").doesNotExist());
    mvc.perform(
            put("/api/teacher/courses/" + id + "/status")
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"HIDDEN\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/public/courses/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/teachers/" + tid))
        .andExpect(jsonPath("$.courses.length()").value(0));
  }

  @Test
  void invalidDataOwnershipRolesAndCsrfAreEnforced() throws Exception {
    long id = create(2);
    var bad = input(2);
    bad.put("endDate", "2029-01-01");
    mvc.perform(
            put("/api/teacher/courses/" + id)
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(bad)))
        .andExpect(status().isBadRequest());
    bad = input(2);
    bad.put("meetings", List.of(Map.of("weekday", 1, "startTime", "19:00", "endTime", "18:00")));
    mvc.perform(
            post("/api/teacher/courses")
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(bad)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/teacher/courses/" + id)
                .with(login("other"))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(input(2))))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/teacher/courses").with(login("student")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/teacher/courses")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/teacher/courses")
                .with(login("teacher"))
                .contentType("application/json")
                .content(json.writeValueAsString(input(2))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/public/courses").param("mode", "INVALID"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/public/courses").param("min", "100").param("max", "1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void applicationsRequireConsentAndRejectDuplicatesAndHiddenCourses() throws Exception {
    long id = create(2);
    publish(id);
    mvc.perform(
            post("/api/student/courses/" + id + "/applications")
                .with(login("student"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"phone\":\"0901234567\"}"))
        .andExpect(status().isBadRequest());
    apply("student", id);
    mvc.perform(
            post("/api/student/courses/" + id + "/applications")
                .with(login("student"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"phone\":\"0901234567\",\"contactConsent\":true}"))
        .andExpect(status().isConflict());
    long hidden = create(2);
    mvc.perform(
            post("/api/student/courses/" + hidden + "/applications")
                .with(login("student2"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"phone\":\"0901234567\",\"contactConsent\":true}"))
        .andExpect(status().isConflict());
  }

  @Test
  void crmHistoryIsPrivateAndCancellationReleasesSeat() throws Exception {
    long id = create(1);
    publish(id);
    long a = apply("student", id);
    assertThat(confirm(a)).isEqualTo(200);
    mvc.perform(get("/api/student/applications").with(login("student")))
        .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
        .andExpect(jsonPath("$[0].history[1].note").isEmpty());
    mvc.perform(get("/api/teacher/applications").with(login("teacher")))
        .andExpect(jsonPath("$[0].history[1].note").value("Private follow-up"));
    mvc.perform(get("/api/teacher/applications").with(login("other")))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            put("/api/teacher/applications/" + a)
                .with(login("other"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"CONFIRMED\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/student/applications/" + a + "/cancel").with(login("student2")).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/student/applications/" + a + "/cancel").with(login("student")).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/public/courses/" + id)).andExpect(jsonPath("$.enrolled").value(0));
    apply("student2", id);
    assertThat(confirm(a)).isEqualTo(409);
  }

  @Test
  void concurrentConfirmationsCannotOverbookAndUpdatesCannotReduceCapacity() throws Exception {
    long id = create(1);
    publish(id);
    long a = apply("student", id), b = apply("student2", id);
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () -> {
                gate.await();
                return confirm(a);
              });
      var second =
          pool.submit(
              () -> {
                gate.await();
                return confirm(b);
              });
      gate.countDown();
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
    mvc.perform(get("/api/public/courses/" + id)).andExpect(jsonPath("$.enrolled").value(1));
    var changed = input(1);
    changed.put("location", "Another location");
    mvc.perform(
            put("/api/teacher/courses/" + id)
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(changed)))
        .andExpect(status().isConflict());
  }

  @Test
  void analyticsReconcilesSourcesAndExcludesOwnerViews() throws Exception {
    long id = create(2);
    publish(id);
    long a = apply("student", id);
    assertThat(confirm(a)).isEqualTo(200);
    String body = "{\"courseId\":" + id + ",\"source\":\"facebook\"}";
    mvc.perform(
            post("/api/public/views").with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/public/views")
                .with(login("teacher"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/teacher/analytics").with(login("teacher")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.views").value(1))
        .andExpect(jsonPath("$.applications").value(1))
        .andExpect(jsonPath("$.confirmed").value(1))
        .andExpect(jsonPath("$.conversionRate").value(100))
        .andExpect(jsonPath("$.sources[0].source").value("facebook"));
    mvc.perform(get("/api/teacher/analytics").with(login("other")))
        .andExpect(jsonPath("$.applications").value(0));
    mvc.perform(
            get("/api/teacher/analytics")
                .with(login("teacher"))
                .param("from", "2020-01-01")
                .param("to", "2030-01-01"))
        .andExpect(status().isBadRequest());
  }
}
