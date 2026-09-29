package az.abb.embassyflow.auth.dto.response;

public record OtpSendResponse(
        String sentTo,
        int expiresInSeconds,
        String demoOtp) {
}