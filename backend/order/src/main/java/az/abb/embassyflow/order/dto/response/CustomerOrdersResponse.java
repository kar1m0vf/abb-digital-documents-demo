package az.abb.embassyflow.order.dto.response;

import java.util.List;

public record CustomerOrdersResponse(
        List<CustomerOrderResponse> orders) {
}
