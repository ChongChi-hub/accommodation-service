package vn.cozynest.communication.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.cozynest.communication.application.port.EmailServicePort;
import vn.cozynest.communication.application.port.UserDirectoryPort;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CommunicationEmailService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationEmailService.class);
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    private final EmailServicePort emailServicePort;
    private final UserDirectoryPort userDirectoryPort;
    private final String webBaseUrl;

    @Autowired
    public CommunicationEmailService(
            EmailServicePort emailServicePort,
            UserDirectoryPort userDirectoryPort,
            @Value("${app.web.base-url:http://localhost:3000}") String webBaseUrl) {
        this.emailServicePort = emailServicePort;
        this.userDirectoryPort = userDirectoryPort;
        this.webBaseUrl = webBaseUrl != null && !webBaseUrl.isBlank() ? webBaseUrl : "http://localhost:3000";
    }

    public record AppointmentEmailPayload(
            String appointmentId,
            String roomId,
            String roomTitle,
            String roomAddress,
            String tenantId,
            String landlordId,
            String contactName,
            String contactPhone,
            String contactEmail,
            String tenantNote,
            String landlordNote,
            String scheduledStart,
            String scheduledEnd,
            String reasonNote) {
    }

    public record RentalRequestEmailPayload(
            String requestId,
            String roomId,
            String tenantId,
            String landlordId,
            String proposedMoveInDate,
            int numOccupants,
            String message,
            String contractId,
            String roomTitle) {
        public RentalRequestEmailPayload(
                String requestId,
                String roomId,
                String tenantId,
                String landlordId,
                String proposedMoveInDate,
                int numOccupants,
                String message,
                String contractId) {
            this(requestId, roomId, tenantId, landlordId, proposedMoveInDate, numOccupants, message, contractId, null);
        }
    }

    public void sendAppointmentCreatedNotification(AppointmentEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> landlordOpt = userDirectoryPort.findUserById(payload.landlordId());
        String toEmail = landlordOpt.map(UserDirectoryPort.UserSummary::email).orElse("landlord@cozynest.vn");
        String landlordName = landlordOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Quý chủ trọ");

        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String tenantName = firstNonBlank(payload.contactName(),
                tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse(null), "Khách thuê");
        String tenantPhone = firstNonBlank(payload.contactPhone(),
                tenantOpt.map(UserDirectoryPort.UserSummary::phone).orElse(null), "-");
        String tenantEmail = firstNonBlank(payload.contactEmail(),
                tenantOpt.map(UserDirectoryPort.UserSummary::email).orElse(null), "-");

        String formattedTime = formatTimestamp(payload.scheduledStart());
        String title = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());
        String subject = "[Tổ Ấm] Có người đặt lịch hẹn xem phòng mới - " + title;

        String contentHtml = """
                <h3 style="color: #0f766e; margin-top: 0;">Lịch hẹn xem phòng mới</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Khách thuê <strong>%s</strong> vừa gửi yêu cầu đặt lịch hẹn xem phòng trực tiếp cho phòng <strong>%s</strong>.</p>
                <div style="background-color: #f5f5f4; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Khách hẹn:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Số điện thoại:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Email khách:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Thời gian hẹn:</strong> <span style="color: #0f766e; font-weight: bold;">%s</span></p>
                    <p style="margin: 4px 0;"><strong>Địa điểm:</strong> %s</p>
                    %s
                </div>
                <p>Vui lòng đăng nhập vào hệ thống Tổ Ấm để xác nhận hoặc từ chối lịch hẹn:</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/dashboard/landlord/appointments" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Quản lý lịch hẹn</a>
                </div>
                """
                .formatted(
                        escapeHtml(landlordName),
                        escapeHtml(tenantName),
                        escapeHtml(title),
                        escapeHtml(tenantName),
                        escapeHtml(tenantPhone),
                        escapeHtml(tenantEmail),
                        escapeHtml(formattedTime),
                        escapeHtml(firstNonBlank(payload.roomAddress(), "-")),
                        payload.tenantNote() != null && !payload.tenantNote().isBlank()
                                ? "<p style='margin: 4px 0;'><strong>Ghi chú của khách:</strong> <em>"
                                        + escapeHtml(payload.tenantNote()) + "</em></p>"
                                : "",
                        webBaseUrl);

        String text = "Lịch hẹn xem phòng mới từ " + tenantName + " vào lúc " + formattedTime + " cho phòng " + title
                + ". Xem tại: " + webBaseUrl + "/dashboard/landlord/appointments";
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendAppointmentConfirmedNotification(AppointmentEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String toEmail = resolveTenantEmail(payload.contactEmail(), payload.tenantId());
        String tenantName = firstNonBlank(payload.contactName(),
                tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse(null), "Quý khách");

        Optional<UserDirectoryPort.UserSummary> landlordOpt = userDirectoryPort.findUserById(payload.landlordId());
        String landlordName = landlordOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Chủ trọ");
        String landlordPhone = firstNonBlank(landlordOpt.map(UserDirectoryPort.UserSummary::phone).orElse(null), "-");

        String formattedTime = formatTimestamp(payload.scheduledStart());
        String title = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());
        String address = firstNonBlank(payload.roomAddress(), "-");
        String subject = "[Tổ Ấm] Chủ nhà đã đồng ý lịch hẹn xem phòng - " + title;

        String contentHtml = """
                <h3 style="color: #0f766e; margin-top: 0;">Lịch hẹn xem phòng đã được chấp thuận!</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Chủ trọ <strong>%s</strong> đã đồng ý lịch hẹn xem phòng <strong>%s</strong> của bạn.</p>
                <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Thời gian hẹn:</strong> <span style="color: #15803d; font-weight: bold;">%s</span></p>
                    <p style="margin: 4px 0;"><strong>Địa điểm:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>SĐT chủ trọ:</strong> %s</p>
                    %s
                </div>
                <p>Vui lòng sắp xếp thời gian đến đúng giờ để buổi xem phòng diễn ra thuận lợi.</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/dashboard/tenant/appointments" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Xem lịch hẹn của tôi</a>
                </div>
                """
                .formatted(
                        escapeHtml(tenantName),
                        escapeHtml(landlordName),
                        escapeHtml(title),
                        escapeHtml(formattedTime),
                        escapeHtml(address),
                        escapeHtml(landlordPhone),
                        payload.landlordNote() != null && !payload.landlordNote().isBlank()
                                ? "<p style='margin: 4px 0;'><strong>Ghi chú từ chủ nhà:</strong> <em>"
                                        + escapeHtml(payload.landlordNote()) + "</em></p>"
                                : "",
                        webBaseUrl);

        String text = "Lịch hẹn xem phòng " + title + " của bạn đã được chủ nhà đồng ý vào lúc " + formattedTime
                + ". Địa điểm: " + address;
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendAppointmentDeclinedNotification(AppointmentEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String toEmail = resolveTenantEmail(payload.contactEmail(), payload.tenantId());
        String tenantName = firstNonBlank(payload.contactName(),
                tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse(null), "Quý khách");

        String formattedTime = formatTimestamp(payload.scheduledStart());
        String title = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());
        String subject = "[Tổ Ấm] Lịch hẹn xem phòng đã bị từ chối - " + title;

        String reason = firstNonBlank(payload.reasonNote(),
                "Chủ trọ bận khung giờ này hoặc phòng đã có người đặt cọc.");

        String contentHtml = """
                <h3 style="color: #b91c1c; margin-top: 0;">Lịch hẹn xem phòng không thể thực hiện</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Rất tiếc, lịch hẹn xem phòng <strong>%s</strong> vào lúc %s của bạn đã bị chủ nhà từ chối hoặc hủy lịch.</p>
                <div style="background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Lý do:</strong> %s</p>
                </div>
                <p>Bạn có thể chọn một khung giờ khác hoặc tìm kiếm các phòng trọ phù hợp khác trên hệ thống Tổ Ấm.</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/rooms" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Khám phá phòng trọ khác</a>
                </div>
                """
                .formatted(
                        escapeHtml(tenantName),
                        escapeHtml(title),
                        escapeHtml(formattedTime),
                        escapeHtml(reason),
                        webBaseUrl);

        String text = "Lịch hẹn xem phòng " + title + " của bạn vào lúc " + formattedTime
                + " đã bị chủ nhà từ chối. Lý do: " + reason;
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendAppointmentCancelledByTenantNotification(AppointmentEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> landlordOpt = userDirectoryPort.findUserById(payload.landlordId());
        String toEmail = landlordOpt.map(UserDirectoryPort.UserSummary::email).orElse("landlord@cozynest.vn");
        String landlordName = landlordOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Quý chủ trọ");

        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String tenantName = firstNonBlank(payload.contactName(),
                tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse(null), "Khách thuê");
        String formattedTime = formatTimestamp(payload.scheduledStart());
        String title = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());
        String reason = firstNonBlank(payload.reasonNote(), "Khách thuê bận đột xuất.");
        String subject = "[Tổ Ấm] Khách thuê đã hủy lịch hẹn xem phòng - " + title;

        String contentHtml = """
                <h3 style="color: #b45309; margin-top: 0;">Thông báo hủy lịch hẹn xem phòng</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Khách thuê <strong>%s</strong> vừa hủy lịch hẹn xem phòng <strong>%s</strong> vào lúc %s.</p>
                <div style="background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Lý do hủy:</strong> %s</p>
                </div>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/dashboard/landlord/appointments" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Xem danh sách lịch hẹn</a>
                </div>
                """
                .formatted(
                        escapeHtml(landlordName),
                        escapeHtml(tenantName),
                        escapeHtml(title),
                        escapeHtml(formattedTime),
                        escapeHtml(reason),
                        webBaseUrl);

        String text = "Khách thuê " + tenantName + " đã hủy lịch hẹn xem phòng " + title + " vào lúc " + formattedTime
                + ".";
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendRentalRequestCreatedNotification(RentalRequestEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> landlordOpt = userDirectoryPort.findUserById(payload.landlordId());
        String toEmail = landlordOpt.map(UserDirectoryPort.UserSummary::email).orElse("landlord@cozynest.vn");
        String landlordName = landlordOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Quý chủ trọ");

        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String tenantName = tenantOpt.map(UserDirectoryPort.UserSummary::fullName)
                .orElse("Khách thuê #" + payload.tenantId());
        String tenantPhone = firstNonBlank(tenantOpt.map(UserDirectoryPort.UserSummary::phone).orElse(null), "-");
        String tenantEmail = firstNonBlank(tenantOpt.map(UserDirectoryPort.UserSummary::email).orElse(null), "-");
        String roomLabel = firstNonBlank(payload.roomTitle(), "phòng #" + payload.roomId());

        String subject = "[Tổ Ấm] Yêu cầu thuê phòng mới - "
                + firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());

        String contentHtml = """
                <h3 style="color: #0f766e; margin-top: 0;">Yêu cầu thuê phòng mới</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Bạn vừa nhận được một yêu cầu thuê phòng mới cho <strong>%s</strong>.</p>
                <div style="background-color: #f5f5f4; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Khách thuê:</strong> %s (SĐT: %s, Email: %s)</p>
                    <p style="margin: 4px 0;"><strong>Phòng:</strong> %s (#%s)</p>
                    <p style="margin: 4px 0;"><strong>Ngày dự kiến dọn vào:</strong> %s</p>
                    <p style="margin: 4px 0;"><strong>Số lượng người ở:</strong> %d người</p>
                    %s
                </div>
                <p>Vui lòng đăng nhập hệ thống để duyệt hoặc từ chối yêu cầu thuê này trong vòng 24 giờ:</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/dashboard/landlord/rental-requests" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Xem yêu cầu thuê phòng</a>
                </div>
                """
                .formatted(
                        escapeHtml(landlordName),
                        escapeHtml(roomLabel),
                        escapeHtml(tenantName),
                        escapeHtml(tenantPhone),
                        escapeHtml(tenantEmail),
                        escapeHtml(roomLabel),
                        escapeHtml(payload.roomId()),
                        escapeHtml(firstNonBlank(payload.proposedMoveInDate(), "Thỏa thuận")),
                        payload.numOccupants() > 0 ? payload.numOccupants() : 1,
                        payload.message() != null && !payload.message().isBlank()
                                ? "<p style='margin: 4px 0;'><strong>Lời nhắn:</strong> <em>"
                                        + escapeHtml(payload.message()) + "</em></p>"
                                : "",
                        webBaseUrl);

        String text = "Bạn nhận được yêu cầu thuê " + roomLabel + " từ khách thuê " + tenantName
                + ". Xem chi tiết tại: " + webBaseUrl + "/dashboard/landlord/rental-requests";
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendRentalRequestApprovedNotification(RentalRequestEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String toEmail = tenantOpt.map(UserDirectoryPort.UserSummary::email).orElse("tenant@cozynest.vn");
        String tenantName = tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Quý khách");
        String roomLabel = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());

        String subject = "[Tổ Ấm] Yêu cầu thuê " + roomLabel + " đã được chấp thuận!";

        String contentHtml = """
                <h3 style="color: #0f766e; margin-top: 0;">Yêu cầu thuê phòng đã được chấp thuận!</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Tin vui! Chủ nhà đã đồng ý yêu cầu thuê <strong>%s</strong> của bạn.</p>
                <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6;">
                    <p style="margin: 4px 0;"><strong>Phòng:</strong> %s (#%s)</p>
                    <p style="margin: 4px 0;"><strong>Hạn giữ chỗ:</strong> <span style="color: #b45309; font-weight: bold;">24 giờ kể từ thời điểm duyệt</span></p>
                    <p style="margin: 4px 0; color: #4b5563;">Hợp đồng thuê phòng đã được khởi tạo tự động. Vui lòng kiểm tra và hoàn tất đặt cọc giữ chỗ để chính thức ký kết.</p>
                </div>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/dashboard/tenant/contracts" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Kiểm tra hợp đồng ngay</a>
                </div>
                """
                .formatted(
                        escapeHtml(tenantName),
                        escapeHtml(roomLabel),
                        escapeHtml(roomLabel),
                        escapeHtml(payload.roomId()),
                        webBaseUrl);

        String text = "Yêu cầu thuê " + roomLabel
                + " của bạn đã được chủ nhà chấp thuận. Vui lòng vào hệ thống kiểm tra hợp đồng giữ chỗ trong 24h: "
                + webBaseUrl + "/dashboard/tenant/contracts";
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    public void sendRentalRequestRejectedNotification(RentalRequestEmailPayload payload) {
        Optional<UserDirectoryPort.UserSummary> tenantOpt = userDirectoryPort.findUserById(payload.tenantId());
        String toEmail = tenantOpt.map(UserDirectoryPort.UserSummary::email).orElse("tenant@cozynest.vn");
        String tenantName = tenantOpt.map(UserDirectoryPort.UserSummary::fullName).orElse("Quý khách");
        String roomLabel = firstNonBlank(payload.roomTitle(), "Phòng #" + payload.roomId());

        String subject = "[Tổ Ấm] Yêu cầu thuê " + roomLabel + " không được chấp thuận";

        String contentHtml = """
                <h3 style="color: #b91c1c; margin-top: 0;">Yêu cầu thuê phòng không thành công</h3>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Rất tiếc, yêu cầu thuê <strong>%s</strong> của bạn đã bị chủ nhà từ chối hoặc phòng đã có người hoàn tất thủ tục trước.</p>
                <p>Bạn có thể tiếp tục tìm kiếm và liên hệ với các chủ trọ có phòng tương tự trên nền tảng Tổ Ấm.</p>
                <div style="text-align: center; margin: 28px 0;">
                    <a href="%s/rooms" style="background-color: #0f766e; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Khám phá phòng trọ khác</a>
                </div>
                """
                .formatted(
                        escapeHtml(tenantName),
                        escapeHtml(roomLabel),
                        webBaseUrl);

        String text = "Yêu cầu thuê " + roomLabel + " của bạn đã bị từ chối. Bạn có thể tìm phòng khác tại "
                + webBaseUrl + "/rooms";
        emailServicePort.sendEmail(toEmail, subject, text, wrapInEmailLayout(subject, contentHtml));
    }

    private String firstNonBlank(String... values) {
        if (values == null)
            return "";
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return "";
    }

    private String resolveTenantEmail(String contactEmail, String tenantId) {
        if (contactEmail != null && !contactEmail.isBlank() && contactEmail.contains("@")) {
            return contactEmail.trim();
        }
        if (tenantId != null && !tenantId.isBlank()) {
            return userDirectoryPort.findUserById(tenantId)
                    .map(UserDirectoryPort.UserSummary::email)
                    .orElse("tenant@cozynest.vn");
        }
        return "tenant@cozynest.vn";
    }

    private String formatTimestamp(String iso) {
        if (iso == null || iso.isBlank())
            return "-";
        try {
            Instant instant = Instant.parse(iso);
            return DATE_TIME_FORMATTER.format(instant.atZone(VN_ZONE));
        } catch (Exception ex) {
            return iso;
        }
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String wrapInEmailLayout(String heading, String innerHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f5f5f4; margin: 0; padding: 24px 12px; color: #292524;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e7e5e4; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                        <div style="background-color: #0f766e; padding: 24px; text-align: center; color: #ffffff;">
                            <h1 style="margin: 0; font-size: 24px; font-weight: bold; letter-spacing: -0.5px;">Tổ Ấm</h1>
                            <p style="margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;">Nền tảng thuê trọ & quản lý phòng trực tuyến minh bạch</p>
                        </div>
                        <div style="padding: 28px 24px;">
                            %s
                        </div>
                        <div style="background-color: #fafaf9; padding: 18px 24px; border-top: 1px solid #f5f5f4; text-align: center; font-size: 12px; color: #78716c;">
                            <p style="margin: 0 0 4px 0;">Đây là email tự động từ hệ thống <strong>Tổ Ấm</strong>. Vui lòng không trả lời trực tiếp email này.</p>
                            <p style="margin: 0;">Cần hỗ trợ? Liên hệ <a href="mailto:hotro@cozynest.vn" style="color: #0f766e; text-decoration: none;">hotro@cozynest.vn</a> hoặc Hotline 1900-CozyNest.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(escapeHtml(heading), innerHtml);
    }
}
