package az.abb.embassyflow.order.dto.response;

import java.util.List;

public record CustomerPaymentsResponse(
        List<PaymentHistoryItemResponse> payments,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}