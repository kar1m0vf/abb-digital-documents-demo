package az.abb.embassyflow.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record DocumentTypeResponse(
        String code,
        String name,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal price,
        String currency) {
}