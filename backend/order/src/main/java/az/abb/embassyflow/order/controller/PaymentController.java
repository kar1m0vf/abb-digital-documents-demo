package az.abb.embassyflow.order.controller;

import az.abb.embassyflow.common.web.AuthAttributes;
import az.abb.embassyflow.order.dto.response.CustomerPaymentsResponse;
import az.abb.embassyflow.order.enums.PaymentStatus;
import az.abb.embassyflow.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Müştəri ödəniş tarixçəsi")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Müştərinin ödəniş tarixçəsi")
    @GetMapping
    public CustomerPaymentsResponse listPayments(@RequestParam Long customerId,
                                                 @RequestParam(required = false) PaymentStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestAttribute(name = AuthAttributes.CUSTOMER_ID,
                                                         required = false) Long authenticatedCustomerId) {
        return paymentService.listPayments(customerId, status, page, size, authenticatedCustomerId);
    }
}