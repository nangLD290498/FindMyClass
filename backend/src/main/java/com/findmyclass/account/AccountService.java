package com.findmyclass.account;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AccountService {
    private final AccountRepository accounts;
    private final TeacherProfileRepository profiles;
    public AccountService(AccountRepository accounts, TeacherProfileRepository profiles) {
        this.accounts = accounts; this.profiles = profiles;
    }
    // Called only with the identity returned by Spring's validated OIDC flow.
    @Transactional
    public void signIn(String subject, String email, String name) {
        Account account = accounts.findByGoogleSubject(subject).orElseGet(() -> new Account(subject, email, name));
        account.updateIdentity(email, name);
        accounts.save(account);
    }
    @Transactional(readOnly = true)
    public UserView current(String subject) { return view(require(subject)); }
    @Transactional
    public UserView chooseRole(String subject, Role role) {
        Account account = accounts.lockBySubject(subject).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (account.getRole() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã chọn vai trò và không thể thay đổi tại đây.");
        account.chooseRole(role);
        return view(account);
    }
    @Transactional
    public UserView completeProfile(String subject, String bio, String subjects) {
        Account account = accounts.lockBySubject(subject).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (account.getRole() != Role.TEACHER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        profiles.saveAndFlush(new TeacherProfile(account.getId(), bio.strip(), subjects.strip()));
        return view(account);
    }
    private Account require(String subject) {
        return accounts.findByGoogleSubject(subject).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private UserView view(Account account) {
        boolean complete = account.getRole() == Role.TEACHER && profiles.existsById(account.getId());
        String next = account.getRole() == null ? "/choose-role" : account.getRole() == Role.STUDENT ? "/student" : complete ? "/teacher" : "/teacher/profile";
        return new UserView(account.getId(), account.getEmail(), account.getDisplayName(), account.getRole(), complete, next);
    }
    public record UserView(Long id, String email, String displayName, Role role, boolean teacherProfileComplete, String nextPath) {}
}
