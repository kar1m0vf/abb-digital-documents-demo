package az.abb.embassyflow.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.notification.dao.entity.Notification;
import az.abb.embassyflow.notification.dao.repository.NotificationRepository;
import az.abb.embassyflow.notification.dto.response.NotificationReadResponse;
import az.abb.embassyflow.notification.dto.response.NotificationResponse;
import az.abb.embassyflow.notification.dto.response.NotificationsResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification(Long id, Long customerId, String title, boolean read) {
        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setTitle(title);
        notification.setBody(title + " body");
        notification.setRead(read);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    void create_persistsAndReturnsNotification() {
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.create(42L, "Sənəd hazırdır", "body");

        assertEquals("Sənəd hazırdır", response.title());
        assertEquals("body", response.body());
        assertFalse(response.read());
    }

    @Test
    void list_ownedCustomer_returnsPageAndUnreadCount() {
        PageImpl<Notification> page = new PageImpl<>(
                List.of(notification(2L, 42L, "Sənəd hazırdır", false),
                        notification(1L, 42L, "Xoş gəldiniz", true)),
                PageRequest.of(0, 10), 2);

        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(42L, PageRequest.of(0, 10)))
                .thenReturn(page);
        when(notificationRepository.countByCustomerIdAndReadFalse(42L)).thenReturn(1L);

        NotificationsResponse response = notificationService.list(42L, 0, 10, 42L);

        assertEquals(2, response.notifications().size());
        assertEquals(1L, response.unreadCount());
        assertEquals(2, response.totalElements());
        assertEquals(2L, response.notifications().get(0).notificationId());
        assertFalse(response.notifications().get(0).read());
        assertTrue(response.notifications().get(1).read());
    }

    @Test
    void list_unauthorized_throws() {
        assertThrows(BusinessException.class, () -> notificationService.list(42L, 0, 10, null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.list(42L, 0, 10, 7L));
        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void markRead_ownedNotification_marksAsRead() {
        Notification notification = notification(5L, 42L, "Sənəd hazırdır", false);
        when(notificationRepository.findByIdAndCustomerId(5L, 42L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationReadResponse response = notificationService.markRead(5L, 42L);

        assertEquals(5L, response.notificationId());
        assertTrue(response.read());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_wrongOwner_throwsNotFound() {
        when(notificationRepository.findByIdAndCustomerId(99L, 42L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markRead(99L, 42L));
        assertEquals(ErrorCodes.NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }
}