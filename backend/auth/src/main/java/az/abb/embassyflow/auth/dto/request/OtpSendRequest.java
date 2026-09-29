package az.abb.embassyflow.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record OtpSendRequest(
        @NotNull Long customerId) {
}