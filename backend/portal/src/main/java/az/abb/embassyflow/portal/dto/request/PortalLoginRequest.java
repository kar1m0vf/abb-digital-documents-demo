package az.abb.embassyflow.portal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PortalLoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}