package az.abb.embassyflow.embassy.dto.response;

public record PortalUserInfo(
        Long id,
        Long embassyId,
        String fullName,
        String role) {
}