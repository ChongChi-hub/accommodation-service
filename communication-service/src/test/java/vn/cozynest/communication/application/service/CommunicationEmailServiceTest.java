package vn.cozynest.communication.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.communication.application.port.EmailServicePort;
import vn.cozynest.communication.application.port.UserDirectoryPort;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunicationEmailServiceTest {

    private EmailServicePort emailServicePort;
    private UserDirectoryPort userDirectoryPort;
    private CommunicationEmailService emailService;

    @BeforeEach
    void setUp() {
        emailServicePort = mock(EmailServicePort.class);
        userDirectoryPort = mock(UserDirectoryPort.class);
        emailService = new CommunicationEmailService(emailServicePort, userDirectoryPort, "http://localhost:3000");

        when(userDirectoryPort.findUserById("u-landlord-01"))
                .thenReturn(Optional.of(new UserDirectoryPort.UserSummary("u-landlord-01", "landlord@cozynest.vn", "Trần Thị Chủ Trọ", "0902000002")));
        when(userDirectoryPort.findUserById("u-tenant-01"))
                .thenReturn(Optional.of(new UserDirectoryPort.UserSummary("u-tenant-01", "tenant@cozynest.vn", "Nguyễn Văn Thuê", "0901000001")));
    }

    @Test
    @DisplayName("Gửi email thông báo cho chủ nhà khi khách thuê đặt lịch hẹn mới")
    void sendsEmailToLandlordOnAppointmentCreated() {
        CommunicationEmailService.AppointmentEmailPayload payload = new CommunicationEmailService.AppointmentEmailPayload(
                "appt-001",
                "room-101",
                "Phòng trọ ban công",
                "15 Duy Tân, Hà Nội",
                "u-tenant-01",
                "u-landlord-01",
                "Nguyễn Văn Thuê",
                "0901000001",
                "tenant@cozynest.vn",
                "Tôi có thể đến sớm 10 phút được không?",
                null,
                "2026-11-20T08:30:00Z",
                "2026-11-20T09:15:00Z",
                null
        );

        emailService.sendAppointmentCreatedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("landlord@cozynest.vn"),
                contains("Có người đặt lịch hẹn xem phòng mới"),
                contains("Lịch hẹn xem phòng mới từ Nguyễn Văn Thuê"),
                argThat(html -> html.contains("Nguyễn Văn Thuê")
                        && html.contains("Phòng trọ ban công")
                        && html.contains("15 Duy Tân, Hà Nội")
                        && html.contains("Tôi có thể đến sớm 10 phút được không?"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho khách thuê khi chủ nhà đồng ý lịch hẹn")
    void sendsEmailToTenantOnAppointmentConfirmed() {
        CommunicationEmailService.AppointmentEmailPayload payload = new CommunicationEmailService.AppointmentEmailPayload(
                "appt-001",
                "room-101",
                "Phòng trọ ban công",
                "15 Duy Tân, Hà Nội",
                "u-tenant-01",
                "u-landlord-01",
                "Nguyễn Văn Thuê",
                "0901000001",
                "tenant@cozynest.vn",
                null,
                "Hẹn gặp bạn tại tầng 1 nhé",
                "2026-11-20T08:30:00Z",
                "2026-11-20T09:15:00Z",
                null
        );

        emailService.sendAppointmentConfirmedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("tenant@cozynest.vn"),
                contains("Chủ nhà đã đồng ý lịch hẹn xem phòng"),
                contains("Lịch hẹn xem phòng Phòng trọ ban công của bạn đã được chủ nhà đồng ý"),
                argThat(html -> html.contains("Lịch hẹn xem phòng đã được chấp thuận!")
                        && html.contains("Hẹn gặp bạn tại tầng 1 nhé"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho khách thuê khi chủ nhà từ chối lịch hẹn")
    void sendsEmailToTenantOnAppointmentDeclined() {
        CommunicationEmailService.AppointmentEmailPayload payload = new CommunicationEmailService.AppointmentEmailPayload(
                "appt-001",
                "room-101",
                "Phòng trọ ban công",
                "15 Duy Tân, Hà Nội",
                "u-tenant-01",
                "u-landlord-01",
                "Nguyễn Văn Thuê",
                "0901000001",
                "tenant@cozynest.vn",
                null,
                null,
                "2026-11-20T08:30:00Z",
                null,
                "Phòng đã có khách cọc trước"
        );

        emailService.sendAppointmentDeclinedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("tenant@cozynest.vn"),
                contains("Lịch hẹn xem phòng đã bị từ chối"),
                contains("đã bị chủ nhà từ chối"),
                argThat(html -> html.contains("Lịch hẹn xem phòng không thể thực hiện")
                        && html.contains("Phòng đã có khách cọc trước"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho chủ nhà khi khách thuê gửi yêu cầu thuê phòng")
    void sendsEmailToLandlordOnRentalRequestCreated() {
        CommunicationEmailService.RentalRequestEmailPayload payload = new CommunicationEmailService.RentalRequestEmailPayload(
                "req-001",
                "room-101",
                "u-tenant-01",
                "u-landlord-01",
                "2026-12-01",
                2,
                "Em là sinh viên muốn thuê ở ghép 2 người",
                null
        );

        emailService.sendRentalRequestCreatedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("landlord@cozynest.vn"),
                contains("Yêu cầu thuê phòng mới"),
                contains("Bạn nhận được yêu cầu thuê phòng #room-101"),
                argThat(html -> html.contains("Nguyễn Văn Thuê")
                        && html.contains("2026-12-01")
                        && html.contains("2 người")
                        && html.contains("Em là sinh viên muốn thuê ở ghép 2 người"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho khách thuê khi chủ nhà chấp thuận yêu cầu thuê phòng")
    void sendsEmailToTenantOnRentalRequestApproved() {
        CommunicationEmailService.RentalRequestEmailPayload payload = new CommunicationEmailService.RentalRequestEmailPayload(
                "req-001",
                "room-101",
                "u-tenant-01",
                "u-landlord-01",
                "2026-12-01",
                2,
                null,
                "ctr-12345"
        );

        emailService.sendRentalRequestApprovedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("tenant@cozynest.vn"),
                contains("đã được chấp thuận"),
                contains("Vui lòng vào hệ thống kiểm tra hợp đồng"),
                argThat(html -> html.contains("Yêu cầu thuê phòng đã được chấp thuận!")
                        && html.contains("24 giờ kể từ thời điểm duyệt"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho khách thuê khi chủ nhà từ chối yêu cầu thuê phòng")
    void sendsEmailToTenantOnRentalRequestRejected() {
        CommunicationEmailService.RentalRequestEmailPayload payload = new CommunicationEmailService.RentalRequestEmailPayload(
                "req-001",
                "room-101",
                "u-tenant-01",
                "u-landlord-01",
                "2026-12-01",
                2,
                null,
                null
        );

        emailService.sendRentalRequestRejectedNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("tenant@cozynest.vn"),
                contains("không được chấp thuận"),
                contains("đã bị từ chối"),
                argThat(html -> html.contains("Yêu cầu thuê phòng không thành công"))
        );
    }

    @Test
    @DisplayName("Gửi email thông báo cho chủ nhà khi khách thuê hủy lịch hẹn xem phòng")
    void sendsEmailToLandlordOnAppointmentCancelledByTenant() {
        CommunicationEmailService.AppointmentEmailPayload payload = new CommunicationEmailService.AppointmentEmailPayload(
                "appt-002",
                "room-101",
                "Phòng Studio Láng Hạ",
                "Số 42 Láng Hạ",
                "u-tenant-01",
                "u-landlord-01",
                "",
                "",
                "",
                null,
                null,
                "2026-11-28T03:30:00Z",
                null,
                "Bận đột xuất"
        );

        emailService.sendAppointmentCancelledByTenantNotification(payload);

        verify(emailServicePort).sendEmail(
                eq("landlord@cozynest.vn"),
                contains("Khách thuê đã hủy lịch hẹn"),
                contains("Nguyễn Văn Thuê"),
                argThat(html -> html.contains("Phòng Studio Láng Hạ")
                        && html.contains("Bận đột xuất"))
        );
    }
}
