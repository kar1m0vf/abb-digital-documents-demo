package az.abb.embassyflow.customer.dto.response;

import az.abb.embassyflow.customer.enums.AccountType;
import az.abb.embassyflow.customer.enums.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;

public record AccountResponse(
        Long id,
        String accountNumber,
        Currency currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal balance,
        AccountType type,
        List<CardResponse> cards) {
}