package az.abb.embassyflow.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FinVerifyRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{7}$") String fin) {
}