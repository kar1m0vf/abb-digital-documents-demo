package az.abb.embassyflow.portal.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.dto.response.AccountResponse;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.customer.service.CustomerService.CustomerInfo;
import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import az.abb.embassyflow.embassy.dao.repository.PortalUserRepository;
import az.abb.embassyflow.notification.service.NotificationService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.OrderItem;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.OrderFilter;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.TimelineStep;
import az.abb.embassyflow.order.service.DocumentService;
import az.abb.embassyflow.portal.dto.request.UpdateDocumentStatusRequest;
import az.abb.embassyflow.portal.dto.response.PortalCustomerResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentDetailResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentItemResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentResponse;
import az.abb.embassyflow.portal.dto.response.PortalDocumentsResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatsResponse;
import az.abb.embassyflow.portal.dto.response.PortalStatusUpdateResponse;
import az.abb.embassyflow.portal.dto.response.PortalTimelineResponse;
import az.abb.embassyflow.portal.dto.response.PortalVerifyResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {

    private static final Set<OrderStatus> COMPLETED_STATUSES =
            Set.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final DocumentOrderRepository orderRepository;
    private final PortalUserRepository portalUserRepository;
    private final CustomerService customerService;
    private final DocumentService documentService;
    private final NotificationService notificationService;

    public PortalService(DocumentOrderRepository orderRepository, PortalUserRepository portalUserRepository,
                         CustomerService customerService, DocumentService documentService,
                         NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.portalUserRepository = portalUserRepository;
        this.customerService = customerService;
        this.documentService = documentService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PortalStatsResponse stats(Long portalUserId) {
        Long embassyId = requireEmbassy(portalUserId);
        long total = orderRepository.countByEmbassyId(embassyId);
        long completed = orderRepository.countByEmbassyIdAndStatusIn(embassyId, COMPLETED_STATUSES);
        long rejected = orderRepository.countByEmbassyIdAndStatus(embassyId, OrderStatus.REJECTED);
        return new PortalStatsResponse(total, completed, total - completed - rejected, rejected);
    }

    @Transactional(readOnly = true)
    public PortalDocumentsResponse documents(String search, OrderFilter filter, int page, int size,
                                              Long portalUserId) {
        Long embassyId = requireEmbassy(portalUserId);
        int pageSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);

        List<PortalDocumentResponse> all = orderRepository.findByEmbassyIdOrderByIdDesc(embassyId).stream()
                .filter(order -> matches(filter, order.getStatus()))
                .map(this::toPortalDocument)
                .filter(document -> isBlank(search) || matchesSearch(document, search))
                .toList();

        int from = (int) Math.min((long) safePage * pageSize, all.size());
        int to = (int) Math.min((long) from + pageSize, all.size());
        List<PortalDocumentResponse> content = from >= to ? List.of() : all.subList(from, to);
        int totalPages = (int) Math.ceil((double) all.size() / pageSize);

        return new PortalDocumentsResponse(content, safePage, pageSize, all.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public PortalDocumentDetailResponse documentDetail(String documentNumber, Long portalUserId) {
        Long embassyId = requireEmbassy(portalUserId);
        DocumentOrder order = findOwned(documentNumber, embassyId);

        PortalCustomerResponse customer = order.getCustomerId() == null ? null
                : customerService.findPortalInfo(order.getCustomerId())
                        .map(info -> new PortalCustomerResponse(info.id(), info.fullName(), info.fin()))
                        .orElse(null);

        Map<Long, AccountResponse> accountsById = accountsByOrder(order);

        List<PortalDocumentItemResponse> items = order.getItems().stream()
                .map(item -> toItemResponse(item, accountsById))
                .toList();

        List<PortalTimelineResponse> timeline = order.getTimeline().stream()
                .map(entry -> new PortalTimelineResponse(entry.getStep(),
                        entry.getStep().description(order.getLanguage()), entry.getCreatedAt()))
                .toList();

        return new PortalDocumentDetailResponse(order.getOrderNumber(), customer, order.getDocumentType().name(),
                viewStatus(order.getStatus()), order.getCreatedAt(), items, timeline);
    }

    @Transactional
    public PortalStatusUpdateResponse updateStatus(String documentNumber, UpdateDocumentStatusRequest request,
                                                    Long portalUserId) {
        Long embassyId = requireEmbassy(portalUserId);
        DocumentOrder order = findOwned(documentNumber, embassyId);

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.REJECTED) {
            throw new BusinessException(ErrorCodes.CONFLICT, "error.conflict", HttpStatus.CONFLICT);
        }

        OrderStatus target = switch (request.status()) {
            case COMPLETED -> OrderStatus.COMPLETED;
            case REJECTED -> OrderStatus.REJECTED;
        };

        if (target == OrderStatus.REJECTED && isBlank(request.note())) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR, "error.rejection_note_required", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(target);
        order.setRejectionNote(target == OrderStatus.REJECTED ? request.note() : null);
        order.addTimeline(TimelineStep.EMBASSY_REVIEWED);
        orderRepository.saveAndFlush(order);

        if (order.getCustomerId() != null) {
            notificationService.create(order.getCustomerId(), title(target, order), body(target, order));
        }

        return new PortalStatusUpdateResponse(order.getOrderNumber(), target.name(), order.getUpdatedAt());
    }

    private static String title(OrderStatus target, DocumentOrder order) {
        boolean az = order.getLanguage() == Language.AZ;
        return switch (target) {
            case COMPLETED -> az ? "Sənəd hazırdır" : "Document is ready";
            case REJECTED -> az ? "Sifariş rədd edildi" : "Order rejected";
            default -> "";
        };
    }

    private static String body(OrderStatus target, DocumentOrder order) {
        boolean az = order.getLanguage() == Language.AZ;
        return switch (target) {
            case COMPLETED -> az
                    ? "Sifarişiniz üzrə sənəd hazırlanıb: " + order.getOrderNumber() + ". Səfirliyə çatdırılıb."
                    : "Your document is ready: " + order.getOrderNumber() + ". Delivered to the embassy.";
            case REJECTED -> az
                    ? "Sifarişiniz rədd edilib: " + order.getOrderNumber() + ". Yenidən müraciət edə bilərsiniz."
                    : "Your order was rejected: " + order.getOrderNumber() + ". You can apply again.";
            default -> "";
        };
    }

    @Transactional(readOnly = true)
    public PortalVerifyResponse verify(String documentNumber, String code) {
        DocumentOrder order = orderRepository.findByOrderNumber(documentNumber)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.DOCUMENT_NOT_FOUND, "error.document_not_found", HttpStatus.NOT_FOUND));

        if (order.getVerificationCode() == null || !order.getVerificationCode().equals(code)) {
            throw new BusinessException(
                    ErrorCodes.INVALID_VERIFICATION_CODE, "error.invalid_verification_code", HttpStatus.BAD_REQUEST);
        }

        boolean valid = order.getStatus() != OrderStatus.REJECTED;
        String customerName = order.getCustomerId() == null ? ""
                : customerService.findById(order.getCustomerId())
                        .map(CustomerInfo::fullName).orElse("");
        String reason = valid ? null : order.getRejectionNote();

        return new PortalVerifyResponse(order.getOrderNumber(), valid, viewStatus(order.getStatus()),
                customerName, order.getDocumentType().name(), order.getCreatedAt(), reason);
    }

    @Transactional
    public ResponseEntity<byte[]> download(String documentNumber, Long portalUserId) {
        Long embassyId = requireEmbassy(portalUserId);
        DocumentOrder order = findOwned(documentNumber, embassyId);

        String html = documentService.renderHtml(order);
        String filename = order.getOrderNumber() + ".html";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private Long requireEmbassy(Long portalUserId) {
        if (portalUserId == null) {
            throw unauthorized();
        }
        PortalUser user = portalUserRepository.findById(portalUserId)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(this::unauthorized);
        return user.getEmbassyId();
    }

    private BusinessException unauthorized() {
        return new BusinessException(ErrorCodes.UNAUTHORIZED, "error.unauthorized", HttpStatus.UNAUTHORIZED);
    }

    private DocumentOrder findOwned(String documentNumber, Long embassyId) {
        return orderRepository.findByOrderNumber(documentNumber)
                .filter(order -> order.getEmbassyId() != null && order.getEmbassyId().equals(embassyId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.DOCUMENT_NOT_FOUND, "error.document_not_found", HttpStatus.NOT_FOUND));
    }

    private PortalDocumentResponse toPortalDocument(DocumentOrder order) {
        String customerName = order.getCustomerId() == null ? ""
                : customerService.findById(order.getCustomerId())
                        .map(CustomerInfo::fullName).orElse("");
        return new PortalDocumentResponse(order.getOrderNumber(), customerName, order.getDocumentType().name(),
                viewStatus(order.getStatus()), order.getCreatedAt());
    }

    private Map<Long, AccountResponse> accountsByOrder(DocumentOrder order) {
        if (order.getCustomerId() == null) {
            return Map.of();
        }
        return customerService.accountsFor(order.getCustomerId(), order.getCustomerId()).stream()
                .collect(Collectors.toMap(AccountResponse::id, Function.identity()));
    }

    private PortalDocumentItemResponse toItemResponse(OrderItem item, Map<Long, AccountResponse> accountsById) {
        AccountResponse account = accountsById.get(item.getAccountId());
        String accountNumber = account == null ? String.valueOf(item.getAccountId()) : account.accountNumber();
        String currency = account == null ? null : account.currency().name();
        return new PortalDocumentItemResponse(accountNumber, currency, item.getPeriod());
    }

    private static boolean matches(OrderFilter filter, OrderStatus status) {
        return switch (filter) {
            case ALL -> true;
            case PENDING -> !COMPLETED_STATUSES.contains(status) && status != OrderStatus.REJECTED;
            case COMPLETED -> COMPLETED_STATUSES.contains(status);
            case REJECTED -> status == OrderStatus.REJECTED;
        };
    }

    private static String viewStatus(OrderStatus status) {
        if (status == OrderStatus.REJECTED) {
            return "REJECTED";
        }
        if (COMPLETED_STATUSES.contains(status)) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    private static boolean matchesSearch(PortalDocumentResponse document, String search) {
        String query = search.trim().toLowerCase();
        return document.documentNumber().toLowerCase().contains(query)
                || document.customerName().toLowerCase().contains(query);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
