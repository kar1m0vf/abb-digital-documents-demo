package az.abb.embassyflow.auth.dto.response;

public record OtpValidateResponse(
        String accessToken,
        String tokenType,
        int expiresInSeconds) {
}