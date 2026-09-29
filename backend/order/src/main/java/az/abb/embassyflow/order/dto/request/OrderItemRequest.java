package az.abb.embassyflow.order.dto.request;

import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long accountId,
        @NotNull Language language,
        @NotNull Period period,
        @NotNull StatementType statementType,
        boolean equivalentCurrency) {
}
