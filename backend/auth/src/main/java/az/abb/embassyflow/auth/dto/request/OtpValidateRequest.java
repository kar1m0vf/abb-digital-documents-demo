package az.abb.embassyflow.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpValidateRequest(
        @NotNull Long customerId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp) {
}