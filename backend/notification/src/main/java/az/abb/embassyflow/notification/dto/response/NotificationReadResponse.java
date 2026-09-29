package az.abb.embassyflow.notification.dto.response;

public record NotificationReadResponse(
        Long notificationId,
        boolean read) {
}