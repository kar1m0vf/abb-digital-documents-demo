package az.abb.embassyflow.notification.controller;

import az.abb.embassyflow.common.web.AuthAttributes;
import az.abb.embassyflow.notification.dto.response.NotificationReadResponse;
import az.abb.embassyflow.notification.dto.response.NotificationsResponse;
import az.abb.embassyflow.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "Müştəri bildiriş mərkəzi")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Bildiriş siyahısı + oxunmamış sayı")
    @GetMapping
    public NotificationsResponse list(@RequestParam Long customerId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                              required = false) Long authenticatedCustomerId) {
        return notificationService.list(customerId, page, size, authenticatedCustomerId);
    }

    @Operation(summary = "Bildirişi oxunmuş kimi işarələ")
    @PatchMapping("/{notificationId}/read")
    public NotificationReadResponse markRead(@PathVariable Long notificationId,
                                             @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                                     required = false) Long authenticatedCustomerId) {
        return notificationService.markRead(notificationId, authenticatedCustomerId);
    }
}