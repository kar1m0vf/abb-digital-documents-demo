package az.abb.embassyflow.portal.dto.request;

import az.abb.embassyflow.portal.enums.PortalDocumentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDocumentStatusRequest(
        @NotNull PortalDocumentStatus status,
        String note) {
}