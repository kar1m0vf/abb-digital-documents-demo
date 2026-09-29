package az.abb.embassyflow.order.controller;

import az.abb.embassyflow.order.dto.response.DocumentTypesResponse;
import az.abb.embassyflow.order.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document Types", description = "Mümkün sənəd növləri")
@RestController
@RequestMapping("/api/v1/document-types")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    public DocumentTypeController(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    @Operation(summary = "Sənəd növlərinin siyahısı")
    @GetMapping
    public DocumentTypesResponse list() {
        return new DocumentTypesResponse(documentTypeService.list());
    }
}