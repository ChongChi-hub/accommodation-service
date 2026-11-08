package vn.cozynest.identity.domain.model;

public enum FaceRejectReasonCode {
    KHONG_KHOP_GIAY_TO("Khuôn mặt không khớp với ảnh trên CCCD/giấy tờ"),
    NGHI_NGO_GIA_MAO("Nghi ngờ giả mạo, hình ảnh cắt ghép hoặc phát lại qua màn hình"),
    ANH_KEM_CHAT_LUONG("Ảnh thiếu sáng, bị mờ, nhòe hoặc không rõ ngũ quan"),
    KHAC("Lý do khác (có ghi chú chi tiết)");

    private final String description;

    FaceRejectReasonCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

