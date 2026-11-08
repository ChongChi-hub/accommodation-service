package vn.cozynest.identity.domain.port;

public class FaceProviderResult {
    private final boolean autoDecided;
    private final boolean passed;
    private final String reasonCode;
    private final String note;

    private FaceProviderResult(boolean autoDecided, boolean passed, String reasonCode, String note) {
        this.autoDecided = autoDecided;
        this.passed = passed;
        this.reasonCode = reasonCode;
        this.note = note;
    }

    public static FaceProviderResult pendingManualReview() {
        return new FaceProviderResult(false, false, null, "Chờ duyệt thủ công bởi kiểm duyệt viên");
    }

    public static FaceProviderResult autoPassed() {
        return new FaceProviderResult(true, true, null, "Vendor eKYC xác thực khuôn mặt thành công");
    }

    public static FaceProviderResult autoFailed(String reasonCode, String note) {
        return new FaceProviderResult(true, false, reasonCode, note);
    }

    public boolean isAutoDecided() { return autoDecided; }
    public boolean isPassed() { return passed; }
    public String getReasonCode() { return reasonCode; }
    public String getNote() { return note; }
}

