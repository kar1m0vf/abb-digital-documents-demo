package az.abb.embassyflow.order.dto.request;

import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.Language;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull DocumentType documentType,
        @NotNull Language language) {
}