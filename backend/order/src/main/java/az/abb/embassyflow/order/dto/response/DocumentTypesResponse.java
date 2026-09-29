package az.abb.embassyflow.order.dto.response;

import java.util.List;

public record DocumentTypesResponse(List<DocumentTypeResponse> documentTypes) {
}