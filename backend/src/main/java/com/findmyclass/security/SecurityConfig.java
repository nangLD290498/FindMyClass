package com.findmyclass.security;
import com.findmyclass.account.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, AccountService accounts, @Value("${app.frontend-url}") String frontend) throws Exception {
        OidcUserService delegate = new OidcUserService();
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/oauth2/**", "/login/**", "/error").permitAll()
                .requestMatchers("/api/auth/me", "/api/onboarding/role", "/api/teacher/**", "/api/student/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
            .oauth2Login(login -> login
                .userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var user = delegate.loadUser(request);
                    if (!Boolean.TRUE.equals(user.getEmailVerified())) throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"));
                    accounts.signIn(user.getSubject(), user.getEmail(), user.getFullName() == null ? user.getEmail() : user.getFullName());
                    return user;
                }))
                .defaultSuccessUrl(frontend + "/auth/callback", true)
                .failureUrl(frontend + "/login?error=google"))
            // Keep default CSRF and session fixation protection enabled.
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
        return http.build();
    }
}
