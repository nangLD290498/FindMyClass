package com.findmyclass;

import com.findmyclass.account.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * Local browser-test fixture only. Not packaged in the production jar. Never deploy test classes.
 */
public class BrowserTestServer {
  public static void main(String[] args) {
    if (!Boolean.getBoolean("findmyclass.browserTests"))
      throw new IllegalStateException("Explicit browser-test opt-in required");
    var app = new SpringApplication(Application.class, Fixtures.class);
    app.setAdditionalProfiles("test", "browser-fixtures");
    app.run(
        "--server.address=127.0.0.1",
        "--server.port=8081",
        "--spring.datasource.url=jdbc:h2:mem:browser;MODE=MySQL;DB_CLOSE_DELAY=-1");
  }

  @TestConfiguration
  static class Fixtures {
    @Bean
    @Order(0)
    SecurityFilterChain fixtureSecurity(HttpSecurity http) throws Exception {
      return http.securityMatcher("/__test/**")
          .authorizeHttpRequests(a -> a.anyRequest().permitAll())
          .build();
    }
  }

  @RestController
  @org.springframework.context.annotation.Profile("browser-fixtures")
  static class FixtureController {
    private final AccountService accounts;

    FixtureController(AccountService accounts) {
      this.accounts = accounts;
    }

    @PostMapping("/__test/login")
    AccountService.UserView login(
        @RequestParam String subject, @RequestParam Role role, HttpServletRequest request) {
      if (!subject.matches("test-[a-zA-Z0-9-]{1,100}"))
        throw new IllegalArgumentException("Fixture subject required");
      accounts.signIn(
          subject, subject + "@example.test", role == Role.TEACHER ? "Cô Mai" : "Minh Anh");
      var user = accounts.current(subject);
      if (user.role() == null) accounts.chooseRole(subject, role);
      if (role == Role.TEACHER)
        accounts.completeProfile(subject, "Giáo viên Toán với 10 năm kinh nghiệm.", "Toán");
      var token =
          new OidcIdToken(
              "fixture-token",
              Instant.now(),
              Instant.now().plusSeconds(3600),
              Map.of("sub", subject));
      var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(
          new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google"));
      request
          .getSession()
          .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      return accounts.current(subject);
    }
  }
}
