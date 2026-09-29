package az.abb.embassyflow.order.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.OrderItem;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dto.request.AddOrderItemsRequest;
import az.abb.embassyflow.order.dto.request.CreateOrderRequest;
import az.abb.embassyflow.order.dto.request.OrderItemRequest;
import az.abb.embassyflow.order.dto.request.UpdateOrderRequest;
import az.abb.embassyflow.order.dto.response.CustomerOrderResponse;
import az.abb.embassyflow.order.dto.response.CustomerOrdersResponse;
import az.abb.embassyflow.order.dto.response.OrderCreatedResponse;
import az.abb.embassyflow.order.dto.response.OrderItemResponse;
import az.abb.embassyflow.order.dto.response.OrderItemsResponse;
import az.abb.embassyflow.order.dto.response.OrderSummaryResponse;
import az.abb.embassyflow.order.dto.response.OrderUpdatedResponse;
import az.abb.embassyflow.order.dto.response.TimelineResponse;
import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.TimelineStep;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final DocumentOrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final EmbassyService embassyService;
    private final CustomerService customerService;

    public OrderService(DocumentOrderRepository orderRepository, OrderNumberGenerator orderNumberGenerator,
                        EmbassyService embassyService, CustomerService customerService) {
        this.orderRepository = orderRepository;
        this.orderNumberGenerator = orderNumberGenerator;
        this.embassyService = embassyService;
        this.customerService = customerService;
    }

    @Transactional
    public OrderCreatedResponse createDraft(CreateOrderRequest request) {
        DocumentOrder order = new DocumentOrder();
        order.setDocumentType(request.documentType());
        order.setLanguage(request.language());
        order.setStatus(OrderStatus.CREATED);
        order.setOrderNumber(orderNumberGenerator.next());
        order.addTimeline(TimelineStep.ORDER_RECEIVED);

        DocumentOrder saved = orderRepository.save(order);

        return new OrderCreatedResponse(saved.getId(), saved.getOrderNumber(), saved.getStatus().name());
    }

    @Transactional
    public OrderUpdatedResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        DocumentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.ORDER_NOT_FOUND, "error.order_not_found", HttpStatus.NOT_FOUND));

        if (order.getDocumentType() == DocumentType.EMBASSY_CERTIFICATE && request.embassyId() == null) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR, "error.embassy_required", HttpStatus.BAD_REQUEST);
        }

        if (request.embassyId() != null && !embassyService.exists(request.embassyId())) {
            throw new BusinessException(
                    ErrorCodes.EMBASSY_NOT_FOUND, "error.embassy_not_found", HttpStatus.NOT_FOUND);
        }

        order.setEmbassyId(request.embassyId());
        order.setLanguage(request.language());

        return new OrderUpdatedResponse(order.getId(), order.getEmbassyId(), order.getLanguage(),
                order.getStatus().name());
    }

    @Transactional
    public OrderUpdatedResponse linkIdentity(Long orderId, Long customerId, Long authenticatedCustomerId) {
        if (authenticatedCustomerId == null || !authenticatedCustomerId.equals(customerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (!customerService.exists(customerId)) {
            throw new BusinessException(
                    ErrorCodes.CUSTOMER_NOT_FOUND, "error.customer_not_found", HttpStatus.NOT_FOUND);
        }

        DocumentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.ORDER_NOT_FOUND, "error.order_not_found", HttpStatus.NOT_FOUND));

        order.setCustomerId(customerId);
        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.OTP_VERIFIED);
            order.addTimeline(TimelineStep.OTP_VERIFIED);
        }

        return new OrderUpdatedResponse(order.getId(), order.getEmbassyId(), order.getLanguage(),
                order.getStatus().name());
    }

    @Transactional
    public OrderItemsResponse addItems(Long orderId, AddOrderItemsRequest request, Long authenticatedCustomerId) {
        DocumentOrder order = requireOwnedOrder(orderId, authenticatedCustomerId);

        if (order.getStatus() != OrderStatus.OTP_VERIFIED) {
            throw new BusinessException(ErrorCodes.CONFLICT, "error.conflict", HttpStatus.CONFLICT);
        }

        for (OrderItemRequest item : request.items()) {
            customerService.validateAccountForCustomer(item.accountId(), order.getCustomerId());
        }

        order.clearItems();
        for (OrderItemRequest item : request.items()) {
            order.addItem(toItem(item));
        }

        orderRepository.flush();

        return new OrderItemsResponse(order.getId(), toItemResponses(order), order.getDocumentType().getPrice());
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrder(Long orderId, Long authenticatedCustomerId) {
        DocumentOrder order = requireOwnedOrder(orderId, authenticatedCustomerId);
        String embassyName = order.getEmbassyId() == null
                ? null
                : embassyService.findName(order.getEmbassyId()).orElse(null);

        return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getDocumentType(),
                order.getStatus().name(), order.getLanguage(), order.getEmbassyId(), embassyName,
                order.getCreatedAt(), order.getDocumentType().getPrice(), toItemResponses(order),
                toTimelineResponses(order));
    }

    DocumentOrder requireOwnedOrder(Long orderId, Long authenticatedCustomerId) {
        DocumentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.ORDER_NOT_FOUND, "error.order_not_found", HttpStatus.NOT_FOUND));

        if (authenticatedCustomerId == null || order.getCustomerId() == null
                || !order.getCustomerId().equals(authenticatedCustomerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return order;
    }

    private static OrderItem toItem(OrderItemRequest request) {
        OrderItem item = new OrderItem();
        item.setAccountId(request.accountId());
        item.setLanguage(request.language());
        item.setPeriod(request.period());
        item.setStatementType(request.statementType());
        item.setEquivalentCurrency(request.equivalentCurrency());
        return item;
    }

    private static List<OrderItemResponse> toItemResponses(DocumentOrder order) {
        return order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getAccountId(), item.getLanguage(),
                        item.getPeriod(), item.getStatementType(), item.isEquivalentCurrency()))
                .toList();
    }

    private static List<TimelineResponse> toTimelineResponses(DocumentOrder order) {
        return order.getTimeline().stream()
                .map(entry -> new TimelineResponse(entry.getStep(),
                        entry.getStep().description(order.getLanguage()), entry.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrdersResponse listOrders(Long customerId, OrderFilter filter, Long authenticatedCustomerId) {
        if (authenticatedCustomerId == null || !authenticatedCustomerId.equals(customerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }

        List<CustomerOrderResponse> orders = orderRepository.findByCustomerIdOrderByIdDesc(customerId).stream()
                .filter(order -> matches(filter, order.getStatus()))
                .map(order -> new CustomerOrderResponse(order.getId(), order.getOrderNumber(),
                        order.getDocumentType(), order.getStatus().name(), order.getCreatedAt(),
                        order.getDocumentType().getPrice()))
                .toList();

        return new CustomerOrdersResponse(orders);
    }

    private static boolean matches(OrderFilter filter, OrderStatus status) {
        return switch (filter) {
            case ALL -> true;
            case PENDING -> status == OrderStatus.OTP_VERIFIED || status == OrderStatus.PAYMENT_RECEIVED
                    || status == OrderStatus.PROCESSING || status == OrderStatus.SIGNED;
            case COMPLETED -> status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED;
            case REJECTED -> status == OrderStatus.REJECTED;
        };
    }
}
