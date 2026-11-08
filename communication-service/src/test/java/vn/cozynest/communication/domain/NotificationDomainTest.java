package vn.cozynest.communication.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.communication.domain.model.Notification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDomainTest {

    @Test
    @DisplayName("Verify Notification creation with recipient, title, and initial unread status")
    void testNotificationCreation() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
                .id("notif-001")
                .userId("u-tenant-01")
                .type("APPOINTMENT_CONFIRMED")
                .title("Lịch xem phòng đã được duyệt")
                .content("Chủ trọ đã đồng ý lịch hẹn vào 14:00 ngày mai")
                .read(false)
                .linkUrl("/dashboard/appointments/app-1")
                .createdAt(now)
                .build();

        assertThat(notification.getId()).isEqualTo("notif-001");
        assertThat(notification.getUserId()).isEqualTo("u-tenant-01");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getTitle()).contains("duyệt");
    }

    @Test
    @DisplayName("Verify Notification read state update with readAt timestamp")
    void testNotificationReadState() {
        LocalDateTime readTime = LocalDateTime.now();
        Notification notification = Notification.builder()
                .id("notif-002")
                .userId("u-tenant-01")
                .title("Thông báo mới")
                .read(true)
                .readAt(readTime)
                .build();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(readTime);
    }
}

