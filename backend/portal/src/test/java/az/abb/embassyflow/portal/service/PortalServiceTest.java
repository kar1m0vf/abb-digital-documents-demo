package az.abb.embassyflow.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.dto.response.AccountResponse;
import az.abb.embassyflow.customer.enums.AccountType;
import az.abb.embassyflow.customer.enums.Currency;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.customer.service.CustomerService.CustomerInfo;
import az.abb.embassyflow.customer.service.CustomerService.CustomerPortalInfo;
import az.abb.embassyflow.embassy.dto.response.PortalUserInfo;
import az.abb.embassyflow.embassy.service.PortalUserService;
import az.abb.embassyflow.notification.service.NotificationService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.OrderItem;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;
import az.abb.embassyflow.order.enums.TimelineStep;
import az.abb.embassyflow.order.service.DocumentService;
import az.abb.embassyflow.portal.dto.request.UpdateDocumentStatusRequest;
import az.abb.embassyflow.portal.dto.response.PortalDocumentDetailResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentItemResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentsResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatsResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatusUpdateResponse;
import az.abb.embassyflow.portal.dto.response.PortalVerifyResponse;
import az.abb.embassyflow.portal.enums.PortalDocumentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock
    private DocumentOrderRepository orderRepository;

    @Mock
    private PortalUserService portalUserService;

    @Mock
    private CustomerService customerService;

    @Mock
    private DocumentService documentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PortalService portalService;

    private static final String DOC = "AR-2026-000512";

    private PortalUserInfo user() {
        return new PortalUserInfo(1L, 1L, "Aydan Əhadova", "ADMIN");
    }

    private DocumentOrder order(OrderStatus status) {
        DocumentOrder order = new DocumentOrder();
        order.setDocumentType(DocumentType.EMBASSY_CERTIFICATE);
        order.setLanguage(Language.AZ);
        order.setStatus(status);
        order.setOrderNumber(DOC);
        order.setEmbassyId(1L);
        order.setCustomerId(1L);
        order.setVerificationCode("FA7K2Q");
        order.setRejectionNote(status == OrderStatus.REJECTED ? "Sənəddəki məlumatlar yanlışdır" : null);
        return order;
    }

    private void addItem(DocumentOrder order) {
        OrderItem item = new OrderItem();
        item.setAccountId(11L);
        item.setLanguage(Language.AZ);
        item.setPeriod(Period.THREE_MONTHS);
        item.setStatementType(StatementType.ALL);
        order.addItem(item);
    }

    private AccountResponse account() {
        return new AccountResponse(11L, "19473526745367352156", Currency.AZN,
                new BigDecimal("12500.50"), AccountType.CURRENT, List.of());
    }

    @Test
    void stats_computesPendingFromTotals() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(orderRepository.countByEmbassyId(1L)).thenReturn(10L);
        when(orderRepository.countByEmbassyIdAndStatusIn(1L, Set.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED)))
                .thenReturn(4L);
        when(orderRepository.countByEmbassyIdAndStatus(1L, OrderStatus.REJECTED)).thenReturn(2L);

        PortalStatsResponse response = portalService.stats(1L);

        assertEquals(10L, response.total());
        assertEquals(4L, response.completed());
        assertEquals(4L, response.pending());
        assertEquals(2L, response.rejected());
    }

    @Test
    void stats_withoutPortalUser_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class, () -> portalService.stats(null));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void documents_filtersPendingAndPaginates() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder pending = order(OrderStatus.PAYMENT_RECEIVED);
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));
        when(orderRepository.findByEmbassyIdOrderByIdDesc(1L))
                .thenReturn(List.of(pending, order(OrderStatus.COMPLETED), order(OrderStatus.REJECTED)));

        PortalDocumentsResponse response = portalService.documents(null, OrderFilter.PENDING, 0, 10, 1L);

        assertEquals(1, response.documents().size());
        assertEquals("AR-2026-000512", response.documents().get(0).documentNumber());
        assertEquals("PENDING", response.documents().get(0).status());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void documents_searchByCustomerName() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.PAYMENT_RECEIVED);
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));
        when(orderRepository.findByEmbassyIdOrderByIdDesc(1L)).thenReturn(List.of(order));

        PortalDocumentsResponse response = portalService.documents("aydan", OrderFilter.ALL, 0, 10, 1L);

        assertEquals(1, response.documents().size());
        assertEquals("Aydan Ahadova", response.documents().get(0).customerName());
    }

    @Test
    void documents_paginationSecondPage() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));
        List<DocumentOrder> orders = List.of(
                order(OrderStatus.PAYMENT_RECEIVED),
                order(OrderStatus.PAYMENT_RECEIVED));
        when(orderRepository.findByEmbassyIdOrderByIdDesc(1L)).thenReturn(orders);

        PortalDocumentsResponse response = portalService.documents(null, OrderFilter.ALL, 1, 1, 1L);

        assertEquals(1, response.documents().size());
        assertEquals(2, response.totalElements());
        assertEquals(2, response.totalPages());
    }

    @Test
    void documents_negativePage_clampsToFirstPage() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));
        when(orderRepository.findByEmbassyIdOrderByIdDesc(1L))
                .thenReturn(List.of(order(OrderStatus.PAYMENT_RECEIVED)));

        PortalDocumentsResponse response = portalService.documents(null, OrderFilter.ALL, -1, 10, 1L);

        assertEquals(0, response.page());
        assertEquals(1, response.documents().size());
        assertEquals(1, response.totalElements());
    }

    @Test
    void documents_pageBeyondRange_returnsEmptyContent() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));
        when(orderRepository.findByEmbassyIdOrderByIdDesc(1L))
                .thenReturn(List.of(order(OrderStatus.PAYMENT_RECEIVED)));

        PortalDocumentsResponse response = portalService.documents(null, OrderFilter.ALL, 99, 10, 1L);

        assertEquals(0, response.documents().size());
        assertEquals(1, response.totalElements());
    }

    @Test
    void documentDetail_returnsEnumNameAndItems() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        addItem(order);
        order.addTimeline(TimelineStep.ORDER_RECEIVED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));
        when(customerService.findPortalInfo(1L))
                .thenReturn(Optional.of(new CustomerPortalInfo(1L, "Aydan Ahadova", "5D7X9Q2")));
        when(customerService.accountsByIds(1L, List.of(11L))).thenReturn(List.of(account()));

        PortalDocumentDetailResponse response = portalService.documentDetail(DOC, 1L);

        assertEquals(DOC, response.documentNumber());
        assertEquals("EMBASSY_CERTIFICATE", response.documentType());
        assertEquals("PENDING", response.status());
        assertEquals("Aydan Ahadova", response.customer().fullName());
        assertEquals("5D7X9Q2", response.customer().fin());
        assertEquals(1, response.items().size());
        PortalDocumentItemResponse item = response.items().get(0);
        assertEquals("19473526745367352156", item.accountNumber());
        assertEquals(Period.THREE_MONTHS, item.period());
        assertEquals(1, response.timeline().size());
        assertEquals(TimelineStep.ORDER_RECEIVED, response.timeline().get(0).step());
    }

    @Test
    void documentDetail_documentOfOtherEmbassy_notFound() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        order.setEmbassyId(2L);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalService.documentDetail(DOC, 1L));

        assertEquals(ErrorCodes.DOCUMENT_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void updateStatus_completed_setsStatusAndTimeline() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));

        PortalStatusUpdateResponse response = portalService.updateStatus(DOC,
                new UpdateDocumentStatusRequest(PortalDocumentStatus.COMPLETED, null), 1L);

        assertEquals("COMPLETED", response.status());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(TimelineStep.EMBASSY_REVIEWED, order.getTimeline().get(0).getStep());
        verify(orderRepository).saveAndFlush(order);
        verify(notificationService).create(1L, "Sənəd hazırdır",
                "Sifarişiniz üzrə sənəd hazırlanıb: AR-2026-000512. Səfirliyə çatdırılıb.");
    }

    @Test
    void updateStatus_rejected_requiresNote() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalService.updateStatus(DOC,
                        new UpdateDocumentStatusRequest(PortalDocumentStatus.REJECTED, null), 1L));

        assertEquals(ErrorCodes.VALIDATION_ERROR, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_rejected_setsRejectionNote() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.DELIVERED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));

        PortalStatusUpdateResponse response = portalService.updateStatus(DOC,
                new UpdateDocumentStatusRequest(PortalDocumentStatus.REJECTED, "Tələblərə uyğun deyil"), 1L);

        assertEquals("REJECTED", response.status());
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertEquals("Tələblərə uyğun deyil", order.getRejectionNote());
        verify(notificationService).create(1L, "Sifariş rədd edildi",
                "Sifarişiniz rədd edilib: AR-2026-000512. Yenidən müraciət edə bilərsiniz.");
    }

    @Test
    void updateStatus_alreadyTerminal_throwsConflict() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order(OrderStatus.COMPLETED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalService.updateStatus(DOC,
                        new UpdateDocumentStatusRequest(PortalDocumentStatus.COMPLETED, null), 1L));

        assertEquals(ErrorCodes.CONFLICT, ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void updateStatus_notDelivered_throwsConflict() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order(OrderStatus.OTP_VERIFIED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalService.updateStatus(DOC,
                        new UpdateDocumentStatusRequest(PortalDocumentStatus.COMPLETED, null), 1L));

        assertEquals(ErrorCodes.CONFLICT, ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void verify_withMatchingCode_isValid() {
        DocumentOrder order = order(OrderStatus.PAYMENT_RECEIVED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));

        PortalVerifyResponse response = portalService.verify(DOC, "FA7K2Q");

        assertTrue(response.valid());
        assertEquals("PENDING", response.status());
        assertEquals("EMBASSY_CERTIFICATE", response.documentType());
        assertEquals("Aydan Ahadova", response.customerName());
    }

    @Test
    void verify_invalidCode_throwsInvalidVerificationCode() {
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order(OrderStatus.PAYMENT_RECEIVED)));

        BusinessException ex = assertThrows(BusinessException.class, () -> portalService.verify(DOC, "WRONG"));

        assertEquals(ErrorCodes.INVALID_VERIFICATION_CODE, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void verify_rejectedOrder_invalidWithReason() {
        DocumentOrder order = order(OrderStatus.REJECTED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));
        when(customerService.findById(1L)).thenReturn(Optional.of(new CustomerInfo(1L, "Aydan Ahadova", "+994...")));

        PortalVerifyResponse response = portalService.verify(DOC, "FA7K2Q");

        assertFalse(response.valid());
        assertEquals("REJECTED", response.status());
        assertEquals("Sənəddəki məlumatlar yanlışdır", response.reason());
    }

    @Test
    void verify_unknownDocument_throwsNotFound() {
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> portalService.verify(DOC, "FA7K2Q"));

        assertEquals(ErrorCodes.DOCUMENT_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void download_returnsHtmlAttachment() {
        when(portalUserService.findActiveById(1L)).thenReturn(user());
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        when(orderRepository.findByOrderNumber(DOC)).thenReturn(Optional.of(order));
        when(documentService.renderHtmlForPortal(DOC, 1L)).thenReturn("<html>ABB</html>");

        ResponseEntity<byte[]> response = portalService.download(DOC, 1L);

        assertNotNull(response.getBody());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition")
                .contains("AR-2026-000512.html"));
    }
}