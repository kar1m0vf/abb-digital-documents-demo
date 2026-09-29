package az.abb.embassyflow.portal.controller;

import az.abb.embassyflow.common.web.AuthAttributes;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.portal.dto.request.PortalLoginRequest;
import az.abb.embassyflow.portal.dto.request.UpdateDocumentStatusRequest;
import az.abb.embassyflow.portal.dto.response.PortalDocumentDetailResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentsResponse;
import az.abb.embassyflow.portal.dto.response.PortalLoginResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatsResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatusUpdateResponse;
import az.abb.embassyflow.portal.dto.response.PortalVerifyResponse;
import az.abb.embassyflow.portal.service.PortalAuthService;
import az.abb.embassyflow.portal.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Embassy Portal", description = "Səfirlik portalı — sifarişlərin idarə edilməsi")
@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final PortalAuthService portalAuthService;
    private final PortalService portalService;

    public PortalController(PortalAuthService portalAuthService, PortalService portalService) {
        this.portalAuthService = portalAuthService;
        this.portalService = portalService;
    }

    @Operation(summary = "Portal girişi (demo: admin@italy / demo1234)")
    @PostMapping("/auth/login")
    public PortalLoginResponse login(@Valid @RequestBody PortalLoginRequest request) {
        return portalAuthService.login(request);
    }

    @Operation(summary = "Portal statistika kartları")
    @GetMapping("/stats")
    public PortalStatsResponse stats(@RequestAttribute(name = AuthAttributes.PORTAL_USER_ID,
            required = false) Long portalUserId) {
        return portalService.stats(portalUserId);
    }

    @Operation(summary = "Sənəd siyahısı (axtarış/filtr/səhifələmə)")
    @GetMapping("/documents")
    public PortalDocumentsResponse documents(@RequestParam(required = false) String search,
                                             @RequestParam(defaultValue = "ALL") OrderFilter status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestAttribute(name = AuthAttributes.PORTAL_USER_ID,
                                                     required = false) Long portalUserId) {
        return portalService.documents(search, status, page, size, portalUserId);
    }

    @Operation(summary = "Sənəd detalları")
    @GetMapping("/documents/{documentNumber}")
    public PortalDocumentDetailResponse documentDetail(@PathVariable String documentNumber,
                                                       @RequestAttribute(name = AuthAttributes.PORTAL_USER_ID,
                                                               required = false) Long portalUserId) {
        return portalService.documentDetail(documentNumber, portalUserId);
    }

    @Operation(summary = "Sənəd statusunu güncəllə (möhür/rədd)")
    @PutMapping("/documents/{documentNumber}/status")
    public PortalStatusUpdateResponse updateStatus(@PathVariable String documentNumber,
                                                   @Valid @RequestBody UpdateDocumentStatusRequest request,
                                                   @RequestAttribute(name = AuthAttributes.PORTAL_USER_ID,
                                                           required = false) Long portalUserId) {
        return portalService.updateStatus(documentNumber, request, portalUserId);
    }

    @Operation(summary = "Sənədin həqiqiliyini təsdiqlə (təhlükəsizlik kodu)")
    @GetMapping("/documents/{documentNumber}/verify")
    public PortalVerifyResponse verify(@PathVariable String documentNumber, @RequestParam String code) {
        return portalService.verify(documentNumber, code);
    }

    @Operation(summary = "Sənədi endir (PDF)")
    @GetMapping("/documents/{documentNumber}/download")
    public ResponseEntity<byte[]> download(@PathVariable String documentNumber,
                                           @RequestAttribute(name = AuthAttributes.PORTAL_USER_ID,
                                                   required = false) Long portalUserId) {
        return portalService.download(documentNumber, portalUserId);
    }
}