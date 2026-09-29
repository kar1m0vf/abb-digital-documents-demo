package az.abb.embassyflow.order.dto.request;

import az.abb.embassyflow.order.enums.Language;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderRequest(
        Long embassyId,
        @NotNull Language language) {
}