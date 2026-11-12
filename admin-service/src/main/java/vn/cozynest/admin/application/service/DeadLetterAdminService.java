package vn.cozynest.admin.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;

import java.util.List;

@Service
public class DeadLetterAdminService {

    private final DeadLetterRepository repository;

    public DeadLetterAdminService(DeadLetterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DeadLetterMessage> listAll() {
        return repository.findAll();
    }

    @Transactional
    public DeadLetterMessage replay(String id, String adminId) {
        DeadLetterMessage msg = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ban ghi DLQ: " + id));
        msg.markReplayed(adminId);
        return repository.save(msg);
    }
}

