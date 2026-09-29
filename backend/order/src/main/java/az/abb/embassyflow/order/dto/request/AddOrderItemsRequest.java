package az.abb.embassyflow.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddOrderItemsRequest(
        @NotEmpty @Valid List<OrderItemRequest> items) {
}
