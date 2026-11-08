package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.application.port.LoginAttemptPort;
import vn.cozynest.identity.infrastructure.persistence.entity.LoginAttemptJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLoginAttemptRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class LoginAttemptAdapter implements LoginAttemptPort {

    private final SpringDataLoginAttemptRepository repository;
    private final Clock clock;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_WINDOW_MINUTES = 15;

    public LoginAttemptAdapter(
            SpringDataLoginAttemptRepository repository,
            @Autowired(required = false) Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public void recordAttempt(String email, String ipAddress, boolean success) {
        repository.save(LoginAttemptJpaEntity.create(email, ipAddress, success));
    }

    @Override
    public boolean isBlocked(String email) {
        return isBlocked(email, null);
    }

    @Override
    public boolean isBlocked(String email, String ipAddress) {
        Instant windowStart = clock.instant().minus(LOCK_WINDOW_MINUTES, ChronoUnit.MINUTES);
        if (email != null && ipAddress != null && !ipAddress.isBlank()) {
            // Anti-DoS: Lock only the specific (email, IP) pair rather than global email
            if (repository.countByEmailAndIpAddressAndSuccessFalseAndAttemptTimeAfter(email, ipAddress, windowStart) >= MAX_FAILED_ATTEMPTS) {
                return true;
            }
        } else if (email != null) {
            if (repository.countByEmailAndSuccessFalseAndAttemptTimeAfter(email, windowStart) >= MAX_FAILED_ATTEMPTS) {
                return true;
            }
        }
        if (ipAddress != null && !ipAddress.isBlank() && repository.countByIpAddressAndSuccessFalseAndAttemptTimeAfter(ipAddress, windowStart) >= (MAX_FAILED_ATTEMPTS * 2)) {
            return true;
        }
        return false;
    }
}
