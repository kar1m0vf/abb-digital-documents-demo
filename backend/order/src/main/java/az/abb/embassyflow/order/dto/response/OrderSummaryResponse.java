package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.Language;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSummaryResponse(
        Long orderId,
        String orderNumber,
        DocumentType documentType,
        String status,
        Language language,
        Long embassyId,
        String embassyName,
        Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal totalAmount,
        List<OrderItemResponse> items,
        List<TimelineResponse> timeline) {
}
