package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record CustomerOrderResponse(
        Long orderId,
        String orderNumber,
        DocumentType documentType,
        String status,
        Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal totalAmount) {
}
