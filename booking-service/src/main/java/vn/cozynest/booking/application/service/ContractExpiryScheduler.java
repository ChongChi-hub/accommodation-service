package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ContractExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContractExpiryScheduler.class);
    private static final int[] REMINDER_THRESHOLDS = {30, 15, 7};

    private final ContractRepository contractRepository;
    private final AppointmentEventOutboxRepository outboxRepository;

    public ContractExpiryScheduler(ContractRepository contractRepository, AppointmentEventOutboxRepository outboxRepository) {
        this.contractRepository = contractRepository;
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    @Transactional
    public int dispatchExpiryReminders() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Contract> expiring = contractRepository.findExpiringBetween(today, today.plusDays(30));
        int count = 0;
        for (Contract contract : expiring) {
            if (!"ACTIVE".equals(contract.getStatus()) || contract.getEndDate() == null) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, contract.getEndDate());
            for (int threshold : REMINDER_THRESHOLDS) {
                if (daysLeft <= threshold && !contract.hasNotifiedDays(threshold)) {
                    contract.markNotifiedDays(threshold);
                    contractRepository.save(contract);
                    String payload = String.format(
                            "{\"contractId\":\"%s\",\"contractCode\":\"%s\",\"tenantId\":\"%s\",\"landlordId\":\"%s\",\"daysRemaining\":%d,\"endDate\":\"%s\"}",
                            contract.getId(), contract.getContractCode(), contract.getTenantId(), contract.getLandlordId(), daysLeft, contract.getEndDate()
                    );
                    outboxRepository.save(new AppointmentEventOutbox("CONTRACT_EXPIRY_REMINDER", contract.getId(), payload));
                    count++;
                    log.info("Queued {}d expiry reminder for contract {}", threshold, contract.getContractCode());
                    break;
                }
            }
        }
        return count;
    }
}

