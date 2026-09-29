package az.abb.embassyflow.notification.dto.response;

import java.util.List;

public record NotificationsResponse(
        List<NotificationResponse> notifications,
        long unreadCount,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}