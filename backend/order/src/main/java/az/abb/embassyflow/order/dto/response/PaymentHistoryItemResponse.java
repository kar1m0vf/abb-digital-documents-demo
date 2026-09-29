package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentHistoryItemResponse(
        Long paymentId,
        String transactionNo,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal amount,
        String currency,
        PaymentStatus status,
        Long orderId,
        String orderNumber,
        Instant createdAt) {
}