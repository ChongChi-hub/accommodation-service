package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MakerCheckerService {
    private static final Logger log = LoggerFactory.getLogger(MakerCheckerService.class);

    private final MakerCheckerRepository repository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    public MakerCheckerService(MakerCheckerRepository repository,
                               LedgerService ledgerService,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MakerCheckerRequest createAdjustmentRequest(String makerId, String accountNumber,
                                                       PostingDirection direction, long amount, String reason) {
        return createAdjustmentRequest(makerId, new LedgerDto.CreateAdjustmentRequest(
                accountNumber, null, direction, amount, reason
        ));
    }

    @Transactional
    public MakerCheckerRequest createAdjustmentRequest(String makerId, LedgerDto.CreateAdjustmentRequest req) {
        if (req.amount() <= 0) {
            throw new IllegalArgumentException("Số tiền điều chỉnh phải lớn hơn 0");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw new IllegalArgumentException("Lý do điều chỉnh không được để trống");
        }
        if (req.accountNumber() == null || req.accountNumber().isBlank()) {
            throw new IllegalArgumentException("Mã tài khoản điều chỉnh không hợp lệ");
        }

        // Tự động suy luận tài khoản đối ứng nếu chưa chỉ định
        String contraAccount = req.contraAccountNumber();
        if (contraAccount == null || contraAccount.isBlank()) {
            if (req.direction() == PostingDirection.CREDIT) {
                // Tăng Có tài khoản đích -> Cần Nợ tài khoản chi phí hoặc tiền gửi
                contraAccount = LedgerService.ACC_EXPENSE;
            } else {
                // Tăng Nợ tài khoản đích -> Cần Có tài khoản doanh thu
                contraAccount = LedgerService.ACC_REVENUE;
            }
        }

        String payloadJson = String.format(
                "{\"accountNumber\":\"%s\",\"contraAccountNumber\":\"%s\",\"direction\":\"%s\",\"amount\":%d}",
                req.accountNumber(), contraAccount, req.direction().name(), req.amount()
        );

        MakerCheckerRequest request = MakerCheckerRequest.create(
                makerId, "MANUAL_ADJUSTMENT", req.amount(), req.reason().trim(), payloadJson
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

        // Parse payload để hạch toán bút toán kép cân đối
        String targetAccount = LedgerService.ACC_GATEWAY;
        String contraAccount = LedgerService.ACC_REVENUE;
        PostingDirection targetDirection = PostingDirection.DEBIT;
        long amount = request.getAmount();

        try {
            if (request.getPayloadJson() != null && !request.getPayloadJson().isBlank()) {
                JsonNode node = objectMapper.readTree(request.getPayloadJson());
                if (node.hasNonNull("accountNumber")) {
                    targetAccount = node.get("accountNumber").asText();
                }
                if (node.hasNonNull("contraAccountNumber")) {
                    contraAccount = node.get("contraAccountNumber").asText();
                }
                if (node.hasNonNull("direction")) {
                    targetDirection = PostingDirection.valueOf(node.get("direction").asText());
                }
                if (node.hasNonNull("amount")) {
                    amount = node.get("amount").asLong();
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi parse payloadJson của yêu cầu {}: {}", requestId, e.getMessage());
        }

        // Định khoản đối ứng đảm bảo Sum(DEBIT) == Sum(CREDIT)
        PostingDirection contraDirection = (targetDirection == PostingDirection.DEBIT)
                ? PostingDirection.CREDIT
                : PostingDirection.DEBIT;

        ledgerService.postEntry(
                "Điều chỉnh thủ công (Duyệt bởi " + checkerId + "): " + request.getReason(),
                "MANUAL_ADJUSTMENT",
                requestId,
                checkerId,
                List.of(
                        new LedgerDto.PostingItem(targetAccount, targetDirection, amount),
                        new LedgerDto.PostingItem(contraAccount, contraDirection, amount)
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

    @Transactional(readOnly = true)
    public List<LedgerDto.AdjustmentResponse> getAllRequests(String status) {
        List<MakerCheckerRequest> list;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            list = repository.findByStatus(status.toUpperCase());
        } else {
            list = repository.findAll();
        }

        List<LedgerDto.AdjustmentResponse> result = new ArrayList<>();
        for (MakerCheckerRequest req : list) {
            String accNum = "";
            String contraAcc = "";
            String dir = "DEBIT";
            try {
                if (req.getPayloadJson() != null && !req.getPayloadJson().isBlank()) {
                    JsonNode node = objectMapper.readTree(req.getPayloadJson());
                    accNum = node.path("accountNumber").asText("");
                    contraAcc = node.path("contraAccountNumber").asText("");
                    dir = node.path("direction").asText("DEBIT");
                }
            } catch (Exception ignored) {}

            result.add(new LedgerDto.AdjustmentResponse(
                    req.getId(),
                    req.getMakerId(),
                    req.getCheckerId(),
                    req.getActionType(),
                    accNum,
                    contraAcc,
                    dir,
                    req.getAmount(),
                    req.getStatus(),
                    req.getReason(),
                    req.getDecidedAt(),
                    req.getCreatedAt()
            ));
        }
        return result;
    }
}
