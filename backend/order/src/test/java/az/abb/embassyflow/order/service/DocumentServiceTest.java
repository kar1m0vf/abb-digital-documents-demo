package az.abb.embassyflow.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.dto.response.AccountResponse;
import az.abb.embassyflow.customer.enums.AccountType;
import az.abb.embassyflow.customer.enums.Currency;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.OrderItem;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dto.response.PreviewResponse;
import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentOrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private CustomerService customerService;

    @Mock
    private EmbassyService embassyService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(orderRepository, orderService, customerService,
                embassyService, "http://localhost:8080/api/v1/portal/documents");
    }

    private DocumentOrder order(OrderStatus status) {
        DocumentOrder order = new DocumentOrder();
        order.setDocumentType(DocumentType.EMBASSY_CERTIFICATE);
        order.setLanguage(Language.AZ);
        order.setStatus(status);
        order.setOrderNumber("AR-2026-000001");
        order.setCustomerId(42L);
        order.setEmbassyId(1L);
        return order;
    }

    private void addItem(DocumentOrder order) {
        OrderItem item = new OrderItem();
        item.setAccountId(11L);
        item.setLanguage(Language.AZ);
        item.setPeriod(Period.THREE_MONTHS);
        item.setStatementType(StatementType.ALL);
        item.setEquivalentCurrency(true);
        order.addItem(item);
    }

    @Test
    void generatePreview_createsCodeHtmlAndQr() {
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        addItem(order);
        when(orderService.requireOwnedOrder(501L, 42L)).thenReturn(order);
        when(orderRepository.existsByVerificationCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(customerService.findById(42L)).thenReturn(Optional.of(
                new CustomerService.CustomerInfo(42L, "Aydan Ahadova", "+994 50 *** ** 82")));
        when(customerService.accountsFor(42L, 42L)).thenReturn(List.of(
                new AccountResponse(11L, "19473526745367352156", Currency.AZN,
                        new BigDecimal("12500.50"), AccountType.CURRENT, List.of())));
        when(embassyService.findName(1L)).thenReturn(Optional.of("İtaliya səfirliyi"));

        PreviewResponse response = documentService.generatePreview(501L, 42L);

        assertEquals("AR-2026-000001", response.documentNumber());
        assertEquals(6, response.verificationCode().length());
        assertEquals(response.verificationCode(), order.getVerificationCode());
        assertTrue(response.htmlPreview().contains("AR-2026-000001"));
        assertTrue(response.htmlPreview().contains("Aydan Ahadova"));
        assertTrue(response.htmlPreview().contains("19473526745367352156"));
        assertTrue(response.qrCodeBase64().startsWith("data:image/png;base64,"));
        assertNotNull(response.qrCodeBase64());
    }

    @Test
    void generatePreview_reusesExistingCode() {
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        addItem(order);
        order.setVerificationCode("EF8F3K");
        when(orderService.requireOwnedOrder(501L, 42L)).thenReturn(order);
        when(customerService.findById(42L)).thenReturn(Optional.empty());
        when(customerService.accountsFor(42L, 42L)).thenReturn(List.of());
        when(embassyService.findName(1L)).thenReturn(Optional.empty());

        PreviewResponse response = documentService.generatePreview(501L, 42L);

        assertEquals("EF8F3K", response.verificationCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void generatePreview_wrongStatus_throwsConflict() {
        DocumentOrder order = order(OrderStatus.CREATED);
        addItem(order);
        when(orderService.requireOwnedOrder(501L, 42L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.generatePreview(501L, 42L));

        assertEquals(ErrorCodes.CONFLICT, ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void generatePreview_withoutItems_throwsConflict() {
        DocumentOrder order = order(OrderStatus.OTP_VERIFIED);
        when(orderService.requireOwnedOrder(501L, 42L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> documentService.generatePreview(501L, 42L));

        assertEquals(ErrorCodes.CONFLICT, ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }
}
