package az.abb.embassyflow.customer.dto.response;

import java.util.List;

public record CustomerAccountsResponse(List<AccountResponse> accounts) {
}