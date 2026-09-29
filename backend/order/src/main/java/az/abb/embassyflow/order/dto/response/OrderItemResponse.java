package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;

public record OrderItemResponse(
        Long orderItemId,
        Long accountId,
        Language language,
        Period period,
        StatementType statementType,
        boolean equivalentCurrency) {
}
