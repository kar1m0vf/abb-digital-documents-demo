package az.abb.embassyflow.portal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record PortalDocumentResponse(
        String documentNumber,
        String customerName,
        String documentType,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant date) {
}