package az.abb.embassyflow.portal.dto.response;

import az.abb.embassyflow.order.enums.Period;

public record PortalDocumentItemResponse(
        String accountNumber,
        String currency,
        Period period) {
}