package az.abb.embassyflow.order.dto.response;

import az.abb.embassyflow.order.enums.TimelineStep;
import java.time.Instant;

public record TimelineResponse(
        TimelineStep step,
        String description,
        Instant timestamp) {
}
