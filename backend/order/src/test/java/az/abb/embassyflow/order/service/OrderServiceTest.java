package az.abb.embassyflow.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dto.request.AddOrderItemsRequest;
import az.abb.embassyflow.order.dto.request.CreateOrderRequest;
import az.abb.embassyflow.order.dto.request.OrderItemRequest;
import az.abb.embassyflow.order.dto.request.UpdateOrderRequest;
import az.abb.embassyflow.order.dto.response.CustomerOrdersResponse;
import az.abb.embassyflow.order.dto.response.OrderCreatedResponse;
import az.abb.embassyflow.order.dto.response.OrderItemsResponse;
import az.abb.embassyflow.order.dto.response.OrderSummaryResponse;
import az.abb.embassyflow.order.dto.response.OrderUpdatedResponse;
import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;
import az.abb.embassyflow.order.enums.TimelineStep;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private DocumentOrderRepository orderRepository;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @Mock
    private EmbassyService embassyService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createDraft_persistsOrderWithNumberAndTimeline() {
        when(orderNumberGenerator.next()).thenReturn("AR-2026-000001");
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(DocumentType.EMBASSY_CERTIFICATE, Language.AZ);
        OrderCreatedResponse response = orderService.createDraft(request);

        ArgumentCaptor<DocumentOrder> captor = ArgumentCaptor.forClass(DocumentOrder.class);
        verify(orderRepository).save(captor.capture());

        DocumentOrder saved = captor.getValue();
        assertEquals("AR-2026-000001", response.orderNumber());
        assertEquals("CREATED", response.status());
        assertEquals(DocumentType.EMBASSY_CERTIFICATE, saved.getDocumentType());
        assertEquals(Language.AZ, saved.getLanguage());
        assertEquals(OrderStatus.CREATED, saved.getStatus());
        assertEquals("AR-2026-000001", saved.getOrderNumber());
        assertEquals(1, saved.getTimeline().size());
        assertEquals(TimelineStep.ORDER_RECEIVED, saved.getTimeline().get(0).getStep());
        assertNotNull(saved.getTimeline().get(0).getOrder());
    }

    private DocumentOrder order(DocumentType type, OrderStatus status) {
        DocumentOrder order = new DocumentOrder();
        order.setDocumentType(type);
        order.setLanguage(Language.AZ);
        order.setStatus(status);
        order.setOrderNumber("AR-2026-000001");
        return order;
    }

    @Test
    void updateOrder_updatesEmbassyAndLanguage() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.CREATED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(embassyService.exists(1L)).thenReturn(true);

        OrderUpdatedResponse response = orderService.updateOrder(501L, new UpdateOrderRequest(1L, Language.EN));

        assertEquals(1L, response.embassyId());
        assertEquals(Language.EN, response.language());
        assertEquals("CREATED", response.status());
        assertEquals(1L, order.getEmbassyId());
        assertEquals(Language.EN, order.getLanguage());
    }

    @Test
    void updateOrder_orderNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(501L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.updateOrder(501L, new UpdateOrderRequest(1L, Language.EN)));

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void updateOrder_certificateWithoutEmbassy_throwsValidationError() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.CREATED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.updateOrder(501L, new UpdateOrderRequest(null, Language.AZ)));

        assertEquals(ErrorCodes.VALIDATION_ERROR, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void updateOrder_unknownEmbassy_throwsEmbassyNotFound() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.CREATED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(embassyService.exists(999L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.updateOrder(501L, new UpdateOrderRequest(999L, Language.AZ)));

        assertEquals(ErrorCodes.EMBASSY_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void linkIdentity_linksCustomerAndAdvancesStatus() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.CREATED);
        when(customerService.exists(42L)).thenReturn(true);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));

        OrderUpdatedResponse response = orderService.linkIdentity(501L, 42L, 42L);

        assertEquals("OTP_VERIFIED", response.status());
        assertEquals(42L, order.getCustomerId());
        assertEquals(OrderStatus.OTP_VERIFIED, order.getStatus());
        assertEquals(1, order.getTimeline().size());
        assertEquals(TimelineStep.OTP_VERIFIED, order.getTimeline().get(0).getStep());
    }

    @Test
    void linkIdentity_missingToken_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.linkIdentity(501L, 42L, null));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void linkIdentity_tokenMismatch_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.linkIdentity(501L, 42L, 43L));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void linkIdentity_unknownCustomer_throwsCustomerNotFound() {
        when(customerService.exists(42L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.linkIdentity(501L, 42L, 42L));

        assertEquals(ErrorCodes.CUSTOMER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void linkIdentity_orderNotFound_throwsOrderNotFound() {
        when(customerService.exists(42L)).thenReturn(true);
        when(orderRepository.findById(501L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.linkIdentity(501L, 42L, 42L));

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    private DocumentOrder verifiedOrder() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.OTP_VERIFIED);
        order.setCustomerId(42L);
        return order;
    }

    private AddOrderItemsRequest singleItemRequest() {
        return new AddOrderItemsRequest(List.of(
                new OrderItemRequest(11L, Language.EN, Period.THREE_MONTHS, StatementType.ALL, true)));
    }

    @Test
    void addItems_replacesItemsAndReturnsPrice() {
        DocumentOrder order = verifiedOrder();
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));

        OrderItemsResponse response = orderService.addItems(501L, singleItemRequest(), 42L);

        assertEquals(1, response.items().size());
        assertEquals(11L, response.items().get(0).accountId());
        assertEquals(Period.THREE_MONTHS, response.items().get(0).period());
        assertEquals(StatementType.ALL, response.items().get(0).statementType());
        assertEquals(new BigDecimal("10.00"), response.totalAmount());
        assertEquals(1, order.getItems().size());
        assertNotNull(order.getItems().get(0).getOrder());
        verify(customerService).validateAccountForCustomer(11L, 42L);
    }

    @Test
    void addItems_wrongStatus_throwsConflict() {
        DocumentOrder order = order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.CREATED);
        order.setCustomerId(42L);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.addItems(501L, singleItemRequest(), 42L));

        assertEquals(ErrorCodes.CONFLICT, ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void addItems_unknownAccount_throwsAccountNotFound() {
        DocumentOrder order = verifiedOrder();
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        doThrow(new BusinessException(ErrorCodes.ACCOUNT_NOT_FOUND, "error.account_not_found",
                HttpStatus.NOT_FOUND)).when(customerService).validateAccountForCustomer(11L, 42L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.addItems(501L, singleItemRequest(), 42L));

        assertEquals(ErrorCodes.ACCOUNT_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void addItems_tokenMismatch_throwsUnauthorized() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(verifiedOrder()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.addItems(501L, singleItemRequest(), 43L));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void getOrder_returnsSummaryWithEmbassyName() {
        DocumentOrder order = verifiedOrder();
        order.setEmbassyId(1L);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(embassyService.findName(1L)).thenReturn(Optional.of("İtaliya səfirliyi"));

        OrderSummaryResponse response = orderService.getOrder(501L, 42L);

        assertEquals("AR-2026-000001", response.orderNumber());
        assertEquals(DocumentType.EMBASSY_CERTIFICATE, response.documentType());
        assertEquals("OTP_VERIFIED", response.status());
        assertEquals(1L, response.embassyId());
        assertEquals("İtaliya səfirliyi", response.embassyName());
        assertEquals(new BigDecimal("10.00"), response.totalAmount());
    }

    @Test
    void getOrder_orderNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(501L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.getOrder(501L, 42L));

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void listOrders_allReturnsEveryOrder() {
        when(orderRepository.findByCustomerIdOrderByIdDesc(42L)).thenReturn(List.of(
                order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.PAYMENT_RECEIVED),
                order(DocumentType.ACCOUNT_STATEMENT, OrderStatus.COMPLETED)));

        CustomerOrdersResponse response = orderService.listOrders(42L, OrderFilter.ALL, 42L);

        assertEquals(2, response.orders().size());
    }

    @Test
    void listOrders_pendingFiltersOutFinalStates() {
        when(orderRepository.findByCustomerIdOrderByIdDesc(42L)).thenReturn(List.of(
                order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.OTP_VERIFIED),
                order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.SIGNED),
                order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.COMPLETED),
                order(DocumentType.EMBASSY_CERTIFICATE, OrderStatus.REJECTED)));

        CustomerOrdersResponse response = orderService.listOrders(42L, OrderFilter.PENDING, 42L);

        assertEquals(2, response.orders().size());
    }

    @Test
    void listOrders_tokenMismatch_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.listOrders(42L, OrderFilter.ALL, 43L));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }
}
