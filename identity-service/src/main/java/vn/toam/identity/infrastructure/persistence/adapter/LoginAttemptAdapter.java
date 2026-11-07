package vn.toam.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.toam.identity.application.port.LoginAttemptPort;
import vn.toam.identity.infrastructure.persistence.entity.LoginAttemptJpaEntity;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataLoginAttemptRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class LoginAttemptAdapter implements LoginAttemptPort {

    private final SpringDataLoginAttemptRepository repository;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_WINDOW_MINUTES = 15;

    public LoginAttemptAdapter(SpringDataLoginAttemptRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordAttempt(String email, String ipAddress, boolean success) {
        repository.save(LoginAttemptJpaEntity.create(email, ipAddress, success));
    }

    @Override
    public boolean isBlocked(String email) {
        Instant windowStart = Instant.now().minus(LOCK_WINDOW_MINUTES, ChronoUnit.MINUTES);
        return repository.countByEmailAndSuccessFalseAndAttemptTimeAfter(email, windowStart) >= MAX_FAILED_ATTEMPTS;
    }
}
