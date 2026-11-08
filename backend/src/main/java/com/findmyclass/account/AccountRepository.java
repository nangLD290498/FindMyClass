package com.findmyclass.account;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByGoogleSubject(String subject);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.googleSubject = :subject")
    Optional<Account> lockBySubject(@Param("subject") String subject);
}
