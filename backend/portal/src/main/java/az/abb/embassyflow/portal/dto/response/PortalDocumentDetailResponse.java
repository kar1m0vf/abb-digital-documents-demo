package az.abb.embassyflow.portal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;

public record PortalDocumentDetailResponse(
        String documentNumber,
        PortalCustomerResponse customer,
        String documentType,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,
        List<PortalDocumentItemResponse> items,
        List<PortalTimelineResponse> timeline) {
}