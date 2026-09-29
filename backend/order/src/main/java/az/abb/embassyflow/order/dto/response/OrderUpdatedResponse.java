package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.Language;

public record OrderUpdatedResponse(
        Long orderId,
        Long embassyId,
        Language language,
        String status) {
}