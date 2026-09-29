package az.abb.embassyflow.order.dto.response;

public record OrderCreatedResponse(Long orderId, String orderNumber, String status) {
}