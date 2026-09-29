package az.abb.embassyflow.portal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record PortalVerifyResponse(
        String documentNumber,
        boolean valid,
        String status,
        String customerName,
        String documentType,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant issuedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reason) {
}