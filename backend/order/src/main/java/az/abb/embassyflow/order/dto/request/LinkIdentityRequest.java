package az.abb.embassyflow.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record LinkIdentityRequest(
        @NotNull Long customerId) {
}