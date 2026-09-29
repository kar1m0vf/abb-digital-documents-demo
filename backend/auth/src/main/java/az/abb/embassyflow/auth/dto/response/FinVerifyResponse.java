package az.abb.embassyflow.auth.dto.response;

public record FinVerifyResponse(
        Long customerId,
        String fullName,
        String phoneMasked) {
}