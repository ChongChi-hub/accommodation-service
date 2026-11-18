package vn.cozynest.admin.application.port;

public interface AdminBillingEscrowPort {
    void freezeEscrow(String contractId, String actorId, String reason);
    void releaseEscrow(String contractId, String actorId);
    void refundEscrow(String contractId, String actorId, String reason);
}
