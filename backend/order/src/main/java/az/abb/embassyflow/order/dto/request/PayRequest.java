package az.abb.embassyflow.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayRequest(
        @NotNull Long cardId,
        @NotBlank String cvv) {
}
