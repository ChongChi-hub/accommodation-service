package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;

import java.util.List;

@Service
public class MakerCheckerService {
    private final MakerCheckerRepository repository;
    private final LedgerService ledgerService;

    public MakerCheckerService(MakerCheckerRepository repository, LedgerService ledgerService) {
        this.repository = repository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public MakerCheckerRequest createAdjustmentRequest(String makerId, String accountNumber,
                                                       PostingDirection direction, long amount, String reason) {
        String payloadJson = String.format("{\"accountNumber\":\"%s\",\"direction\":\"%s\",\"amount\":%d}",
                accountNumber, direction, amount);
        MakerCheckerRequest request = MakerCheckerRequest.create(
                makerId, "MANUAL_ADJUSTMENT", amount, reason, payloadJson
        );
        return repository.save(request);
    }

    @Transactional
    public void approve(String requestId, String checkerId) {
        MakerCheckerRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu duyệt: " + requestId));

        // Enforce maker != checker rule
        request.approve(checkerId);
        repository.save(request);

        // Execute manual adjustment with compensatory contra-account
        ledgerService.postEntry(
                "Điều chỉnh thủ công (Duyệt bởi " + checkerId + "): " + request.getReason(),
                "MANUAL_ADJUSTMENT",
                requestId,
                checkerId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, request.getAmount()),
                        new LedgerDto.PostingItem(LedgerService.ACC_REVENUE, PostingDirection.CREDIT, request.getAmount())
                )
        );
    }

    @Transactional
    public void reject(String requestId, String checkerId, String reason) {
        MakerCheckerRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu duyệt: " + requestId));

        request.reject(checkerId, reason);
        repository.save(request);
    }

    @Transactional(readOnly = true)
    public List<MakerCheckerRequest> getPendingRequests() {
        return repository.findByStatus("PENDING");
    }
}

