package com.findmyclass.security;
import com.findmyclass.account.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
@Component("accountAccess")
public class AccountAccess {
    private final AccountRepository accounts;
    private final TeacherProfileRepository profiles;
    public AccountAccess(AccountRepository accounts, TeacherProfileRepository profiles) { this.accounts = accounts; this.profiles = profiles; }
    public boolean hasRole(Authentication auth, String role) {
        if (auth == null || !(auth.getPrincipal() instanceof OidcUser user)) return false;
        return accounts.findByGoogleSubject(user.getSubject()).map(a -> a.getRole() != null && a.getRole().name().equals(role)).orElse(false);
    }
    public boolean teacherReady(Authentication auth) {
        if (!hasRole(auth, "TEACHER")) return false;
        OidcUser user = (OidcUser) auth.getPrincipal();
        return accounts.findByGoogleSubject(user.getSubject()).map(a -> profiles.existsById(a.getId())).orElse(false);
    }
}
