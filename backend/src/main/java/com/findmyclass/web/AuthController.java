package com.findmyclass.web;
import com.findmyclass.account.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class AuthController {
    private final AccountService accounts;
    public AuthController(AccountService accounts) { this.accounts = accounts; }
    @GetMapping("/api/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }
    @GetMapping("/api/auth/me")
    public AccountService.UserView me(@AuthenticationPrincipal OidcUser user) { return accounts.current(user.getSubject()); }
    @PostMapping("/api/onboarding/role")
    public AccountService.UserView role(@AuthenticationPrincipal OidcUser user, @Valid @RequestBody RoleRequest request) { return accounts.chooseRole(user.getSubject(), request.role()); }
    @PutMapping("/api/teacher/profile")
    @PreAuthorize("@accountAccess.hasRole(authentication, 'TEACHER')")
    public AccountService.UserView profile(@AuthenticationPrincipal OidcUser user, @Valid @RequestBody ProfileRequest request) { return accounts.completeProfile(user.getSubject(), request.bio(), request.subjects()); }
    @GetMapping("/api/teacher/dashboard")
    @PreAuthorize("@accountAccess.teacherReady(authentication)")
    public Map<String, String> teacher() { return Map.of("message", "Tài khoản giáo viên của bạn đã sẵn sàng."); }
    @GetMapping("/api/student/dashboard")
    @PreAuthorize("@accountAccess.hasRole(authentication, 'STUDENT')")
    public Map<String, String> student() { return Map.of("message", "Tài khoản học viên của bạn đã sẵn sàng."); }
    public record RoleRequest(@NotNull Role role) {}
    public record ProfileRequest(@NotBlank @Size(max = 2000) String bio, @NotBlank @Size(max = 500) String subjects) {}
}
