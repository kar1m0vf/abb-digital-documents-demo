package az.abb.embassyflow.notification.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.notification.dao.entity.Notification;
import az.abb.embassyflow.notification.dao.repository.NotificationRepository;
import az.abb.embassyflow.notification.dto.response.NotificationReadResponse;
import az.abb.embassyflow.notification.dto.response.NotificationResponse;
import az.abb.embassyflow.notification.dto.response.NotificationsResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse create(Long customerId, String title, String body) {
        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setTitle(title);
        notification.setBody(body);

        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public NotificationsResponse list(Long customerId, int page, int size, Long authenticatedCustomerId) {
        if (authenticatedCustomerId == null || !authenticatedCustomerId.equals(customerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<Notification> result = notificationRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        long unreadCount = notificationRepository.countByCustomerIdAndReadFalse(customerId);

        List<NotificationResponse> notifications = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new NotificationsResponse(notifications, unreadCount, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public NotificationReadResponse markRead(Long notificationId, Long authenticatedCustomerId) {
        if (authenticatedCustomerId == null) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Notification notification = notificationRepository.findByIdAndCustomerId(notificationId,
                        authenticatedCustomerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND, "error.notification_not_found", HttpStatus.NOT_FOUND));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return new NotificationReadResponse(notification.getId(), notification.isRead());
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(),
                notification.getBody(), notification.isRead(), notification.getCreatedAt());
    }
}