package az.abb.embassyflow.portal.dto.response;

import java.util.List;

public record PortalDocumentsResponse(
        List<PortalDocumentResponse> documents,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}