package az.abb.embassyflow.notification.dto.response;

import java.time.Instant;

public record NotificationResponse(
        Long notificationId,
        String title,
        String body,
        boolean read,
        Instant createdAt) {
}