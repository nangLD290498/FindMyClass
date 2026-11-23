package com.findmyclass;

import com.findmyclass.account.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired AccountRepository repository;
    @Autowired TeacherProfileRepository profiles;

    @BeforeEach void reset() { profiles.deleteAll(); repository.deleteAll(); }
    void newUser(String subject) { accounts.signIn(subject, subject + "@example.com", "Test User"); }
    org.springframework.test.web.servlet.request.RequestPostProcessor login(String subject) {
        return oidcLogin().idToken(t -> t.subject(subject).claim("email", subject + "@example.com").claim("email_verified", true));
    }
    @Test void anonymousRequestsRequireAuthenticationAndGoogleRedirectUsesStateAndNonce() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/student/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        String redirect = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("https://accounts.google.com/").contains("state=", "nonce=", "scope=openid", "redirect_uri=");
    }
    @Test void invalidOAuthCallbackCannotCreateAnAccount() throws Exception {
        mvc.perform(get("/login/oauth2/code/google").param("code", "untrusted").param("state", "untrusted"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:5173/login?error=google"));
        assertThat(repository.count()).isZero();
    }
    @Test void newAccountCannotAccessDashboardsUntilRoleIsSelected() throws Exception {
        newUser("new");
        mvc.perform(get("/api/auth/me").with(login("new"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.role").isEmpty()).andExpect(jsonPath("$.nextPath").value("/choose-role"));
        mvc.perform(get("/api/teacher/dashboard").with(login("new"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/student/dashboard").with(login("new"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/teacher/profile").with(login("new")).with(csrf()).contentType("application/json")
            .content("{\"bio\":\"About me\",\"subjects\":\"Math\"}")).andExpect(status().isForbidden());
    }
    @Test void studentRolePersistsAndCannotBeChanged() throws Exception {
        newUser("student");
        mvc.perform(post("/api/onboarding/role").with(login("student")).with(csrf()).contentType("application/json")
            .content("{\"role\":\"STUDENT\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.nextPath").value("/student"));
        mvc.perform(get("/api/student/dashboard").with(login("student"))).andExpect(status().isOk());
        mvc.perform(get("/api/teacher/dashboard").with(login("student"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/teacher/profile").with(login("student")).with(csrf()).contentType("application/json")
            .content("{\"bio\":\"About me\",\"subjects\":\"Math\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/onboarding/role").with(login("student")).with(csrf()).contentType("application/json")
            .content("{\"role\":\"TEACHER\"}")).andExpect(status().isConflict());
        accounts.signIn("student", "changed@example.com", "New name");
        assertThat(accounts.current("student").role()).isEqualTo(Role.STUDENT);
        assertThat(accounts.current("student").email()).isEqualTo("changed@example.com");
        assertThat(repository.count()).isEqualTo(1);
    }
    @Test void teacherMustCompleteProfileAndCannotAccessStudentDashboard() throws Exception {
        newUser("teacher");
        mvc.perform(post("/api/onboarding/role").with(login("teacher")).with(csrf()).contentType("application/json")
            .content("{\"role\":\"TEACHER\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.nextPath").value("/teacher/profile"));
        mvc.perform(get("/api/teacher/dashboard").with(login("teacher"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/teacher/profile").with(login("teacher")).with(csrf()).contentType("application/json")
            .content("{\"bio\":\"  \",\"subjects\":\"Math\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/teacher/profile").with(login("teacher")).with(csrf()).contentType("application/json")
            .content("{\"bio\":\"I teach math.\",\"subjects\":\"Mathematics\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.teacherProfileComplete").value(true)).andExpect(jsonPath("$.nextPath").value("/teacher"));
        mvc.perform(get("/api/teacher/dashboard").with(login("teacher"))).andExpect(status().isOk());
        mvc.perform(get("/api/student/dashboard").with(login("teacher"))).andExpect(status().isForbidden());
    }
    @Test void roleMustBeValidAndMutationsRequireCsrf() throws Exception {
        newUser("new");
        mvc.perform(post("/api/onboarding/role").with(login("new")).contentType("application/json")
            .content("{\"role\":\"STUDENT\"}")).andExpect(status().isForbidden());
        for (String body : List.of("{}", "{\"role\":\"ADMIN\"}")) {
            mvc.perform(post("/api/onboarding/role").with(login("new")).with(csrf()).contentType("application/json")
                .content(body)).andExpect(status().isBadRequest());
        }
        assertThat(accounts.current("new").role()).isNull();
    }
    @Test void subjectIdentifiesAccountsRatherThanEmail() {
        accounts.signIn("one", "same@example.com", "One");
        accounts.signIn("ONE", "same@example.com", "Two");
        assertThat(repository.count()).isEqualTo(2);
        assertThat(accounts.current("one").id()).isNotEqualTo(accounts.current("ONE").id());
    }
    @Test void concurrentRoleChoicesHaveOnlyOneWinner() throws Exception {
        newUser("race");
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(Role.TEACHER, Role.STUDENT).stream().map(role -> executor.submit(() -> {
                gate.await();
                try { accounts.chooseRole("race", role); return 200; }
                catch (org.springframework.web.server.ResponseStatusException ex) { return ex.getStatusCode().value(); }
            })).toList();
            gate.countDown();
            assertThat(List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200, 409);
        }
    }
    @Test void sessionRestoresUserAndLogoutInvalidatesIt() throws Exception {
        newUser("session");
        var token = new OidcIdToken("test-token", Instant.now(), Instant.now().plusSeconds(600), Map.of("sub", "session"));
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "google"));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
