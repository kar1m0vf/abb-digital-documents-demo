package az.abb.embassyflow.portal.dto.response;

import az.abb.embassyflow.order.enums.TimelineStep;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record PortalTimelineResponse(
        TimelineStep step,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp) {
}