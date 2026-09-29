package az.abb.embassyflow.order.controller;

import az.abb.embassyflow.common.web.AuthAttributes;
import az.abb.embassyflow.order.dto.request.AddOrderItemsRequest;
import az.abb.embassyflow.order.dto.request.CreateOrderRequest;
import az.abb.embassyflow.order.dto.request.LinkIdentityRequest;
import az.abb.embassyflow.order.dto.request.PayRequest;
import az.abb.embassyflow.order.dto.request.UpdateOrderRequest;
import az.abb.embassyflow.order.dto.response.CustomerOrdersResponse;
import az.abb.embassyflow.order.dto.response.OrderCreatedResponse;
import az.abb.embassyflow.order.dto.response.OrderItemsResponse;
import az.abb.embassyflow.order.dto.response.OrderSummaryResponse;
import az.abb.embassyflow.order.dto.response.OrderUpdatedResponse;
import az.abb.embassyflow.order.dto.response.PaymentResponse;
import az.abb.embassyflow.order.dto.response.PreviewResponse;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.order.service.DocumentService;
import az.abb.embassyflow.order.service.OrderService;
import az.abb.embassyflow.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Sifariş (draft → sənəd → ödəniş) axını")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final DocumentService documentService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, DocumentService documentService,
                           PaymentService paymentService) {
        this.orderService = orderService;
        this.documentService = documentService;
        this.paymentService = paymentService;
    }

    @Operation(summary = "Yeni sifariş draftı yarat")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreatedResponse createDraft(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createDraft(request);
    }

    @Operation(summary = "Sifarişi güncəllə")
    @PutMapping("/{orderId}")
    public OrderUpdatedResponse update(@PathVariable Long orderId, @Valid @RequestBody UpdateOrderRequest request) {
        return orderService.updateOrder(orderId, request);
    }

    @Operation(summary = "Sifarişə FİN/şəxsiyyət əlaqələndir")
    @PutMapping("/{orderId}/identity")
    public OrderUpdatedResponse linkIdentity(@PathVariable Long orderId,
                                             @Valid @RequestBody LinkIdentityRequest request,
                                             @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                                     required = false) Long authenticatedCustomerId) {
        return orderService.linkIdentity(orderId, request.customerId(), authenticatedCustomerId);
    }

    @Operation(summary = "Sənəd detallarını (hesab/dövr) əlavə et")
    @PostMapping("/{orderId}/items")
    public OrderItemsResponse addItems(@PathVariable Long orderId,
                                       @Valid @RequestBody AddOrderItemsRequest request,
                                       @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                               required = false) Long authenticatedCustomerId) {
        return orderService.addItems(orderId, request, authenticatedCustomerId);
    }

    @Operation(summary = "Sifarişi əldə et")
    @GetMapping("/{orderId}")
    public OrderSummaryResponse getOrder(@PathVariable Long orderId,
                                         @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                                 required = false) Long authenticatedCustomerId) {
        return orderService.getOrder(orderId, authenticatedCustomerId);
    }

    @Operation(summary = "Müştərinin sifariş siyahısı")
    @GetMapping
    public CustomerOrdersResponse listOrders(@RequestParam Long customerId,
                                              @RequestParam(defaultValue = "ALL") OrderFilter status,
                                              @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                                      required = false) Long authenticatedCustomerId) {
        return orderService.listOrders(customerId, status, authenticatedCustomerId);
    }

    @Operation(summary = "Sənəd önizləməsini generasiya et")
    @PostMapping("/{orderId}/preview")
    public PreviewResponse preview(@PathVariable Long orderId,
                                   @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                           required = false) Long authenticatedCustomerId) {
        return documentService.generatePreview(orderId, authenticatedCustomerId);
    }

    @Operation(summary = "Sifarişi ödə")
    @PostMapping("/{orderId}/pay")
    public PaymentResponse pay(@PathVariable Long orderId,
                               @Valid @RequestBody PayRequest request,
                               @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                       required = false) Long authenticatedCustomerId) {
        return paymentService.pay(orderId, request, authenticatedCustomerId);
    }
}