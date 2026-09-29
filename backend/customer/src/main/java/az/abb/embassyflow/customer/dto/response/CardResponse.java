package az.abb.embassyflow.customer.dto.response;

public record CardResponse(
        Long id,
        String maskedNumber,
        String brand,
        String expiry) {
}