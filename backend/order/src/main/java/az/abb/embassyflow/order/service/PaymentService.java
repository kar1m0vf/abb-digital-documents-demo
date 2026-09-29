package az.abb.embassyflow.order.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.Payment;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dao.repository.PaymentRepository;
import az.abb.embassyflow.order.dto.request.PayRequest;
import az.abb.embassyflow.order.dto.response.CustomerPaymentsResponse;
import az.abb.embassyflow.order.dto.response.PaymentHistoryItemResponse;
import az.abb.embassyflow.order.dto.response.PaymentResponse;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.PaymentStatus;
import az.abb.embassyflow.order.enums.TimelineStep;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DocumentOrderRepository orderRepository;
    private final TransactionNumberGenerator transactionNumberGenerator;
    private final CustomerService customerService;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository, DocumentOrderRepository orderRepository,
                          TransactionNumberGenerator transactionNumberGenerator,
                          CustomerService customerService, OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.transactionNumberGenerator = transactionNumberGenerator;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponse pay(Long orderId, PayRequest request, Long authenticatedCustomerId) {
        DocumentOrder order = orderService.requireOwnedOrder(orderId, authenticatedCustomerId);

        if (order.getStatus() != OrderStatus.OTP_VERIFIED || order.getItems().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.PAYMENT_FAILED, "error.payment_failed", HttpStatus.BAD_REQUEST);
        }

        customerService.validateCardForCustomer(request.cardId(), order.getCustomerId());

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setCardId(request.cardId());
        payment.setAmount(order.getDocumentType().getPrice());
        payment.setCurrency(order.getDocumentType().getCurrency());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionNo(transactionNumberGenerator.next());

        Payment saved = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAYMENT_RECEIVED);
        order.addTimeline(TimelineStep.PAYMENT_RECEIVED);

        return new PaymentResponse(saved.getId(), saved.getTransactionNo(), saved.getAmount(),
                saved.getCurrency(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public CustomerPaymentsResponse listPayments(Long customerId, PaymentStatus status, int page, int size,
                                                 Long authenticatedCustomerId) {
        if (authenticatedCustomerId == null || !authenticatedCustomerId.equals(customerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
        }

        int safePage = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        List<Long> orderIds = orderRepository.findByCustomerIdOrderByIdDesc(customerId).stream()
                .map(DocumentOrder::getId)
                .toList();

        if (orderIds.isEmpty()) {
            return new CustomerPaymentsResponse(List.of(), pageable.getPageNumber(),
                    pageable.getPageSize(), 0, 0);
        }

        Page<Payment> result = status == null
                ? paymentRepository.findByOrderIdIn(orderIds, pageable)
                : paymentRepository.findByOrderIdInAndStatus(orderIds, status, pageable);

        Set<Long> distinctOrderIds = result.getContent().stream()
                .map(Payment::getOrderId)
                .collect(Collectors.toSet());
        Map<Long, String> orderNumbers = orderRepository.findAllById(distinctOrderIds).stream()
                .collect(Collectors.toMap(DocumentOrder::getId, DocumentOrder::getOrderNumber));

        List<PaymentHistoryItemResponse> payments = result.getContent().stream()
                .map(p -> new PaymentHistoryItemResponse(p.getId(), p.getTransactionNo(), p.getAmount(),
                        p.getCurrency(), p.getStatus(), p.getOrderId(),
                        orderNumbers.get(p.getOrderId()), p.getCreatedAt()))
                .toList();

        return new CustomerPaymentsResponse(payments, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
