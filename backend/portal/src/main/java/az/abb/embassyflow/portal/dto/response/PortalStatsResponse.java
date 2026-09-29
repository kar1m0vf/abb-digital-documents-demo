package az.abb.embassyflow.portal.dto.response;

public record PortalStatsResponse(
        long total,
        long completed,
        long pending,
        long rejected) {
}