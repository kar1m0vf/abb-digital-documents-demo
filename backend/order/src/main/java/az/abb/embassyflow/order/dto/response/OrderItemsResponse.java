package az.abb.embassyflow.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;

public record OrderItemsResponse(
        Long orderId,
        List<OrderItemResponse> items,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal totalAmount) {
}
