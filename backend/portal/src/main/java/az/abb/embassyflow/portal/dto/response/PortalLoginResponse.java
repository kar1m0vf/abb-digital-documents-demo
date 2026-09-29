package az.abb.embassyflow.portal.dto.response;

public record PortalLoginResponse(
        String accessToken,
        String tokenType,
        String embassyName,
        String userName,
        String role) {
}