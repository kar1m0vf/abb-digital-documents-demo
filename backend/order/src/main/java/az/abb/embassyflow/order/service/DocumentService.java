package az.abb.embassyflow.order.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.dto.response.AccountResponse;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.order.dao.entity.DocumentOrder;
import az.abb.embassyflow.order.dao.entity.OrderItem;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dto.response.PreviewResponse;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.OrderStatus;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int QR_SIZE = 240;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final DocumentOrderRepository orderRepository;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final EmbassyService embassyService;
    private final MessageSource messageSource;
    private final String verifyBaseUrl;

    public DocumentService(DocumentOrderRepository orderRepository, OrderService orderService,
                           CustomerService customerService, EmbassyService embassyService,
                           MessageSource messageSource,
                           @Value("${app.verify-base-url:http://localhost:8080/api/v1/portal/documents}")
                           String verifyBaseUrl) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.customerService = customerService;
        this.embassyService = embassyService;
        this.messageSource = messageSource;
        this.verifyBaseUrl = verifyBaseUrl;
    }

    @Transactional
    public PreviewResponse generatePreview(Long orderId, Long authenticatedCustomerId) {
        DocumentOrder order = orderService.requireOwnedOrder(orderId, authenticatedCustomerId);

        if (order.getStatus() != OrderStatus.OTP_VERIFIED || order.getItems().isEmpty()) {
            throw new BusinessException(ErrorCodes.CONFLICT, "error.conflict", HttpStatus.CONFLICT);
        }

        String verificationCode = ensureVerificationCode(order);
        String html = buildHtml(order, verificationCode);
        String qrCode = toQrDataUri(verifyBaseUrl + "/" + order.getOrderNumber() + "/verify?code=" + verificationCode);

        return new PreviewResponse(order.getOrderNumber(), html, qrCode, verificationCode);
    }

    public String renderHtmlForPortal(String documentNumber, Long embassyId) {
        DocumentOrder order = requireOwnedByEmbassy(documentNumber, embassyId);
        String code = ensureVerificationCode(order);
        return buildHtml(order, code);
    }

    private DocumentOrder requireOwnedByEmbassy(String documentNumber, Long embassyId) {
        return orderRepository.findByOrderNumber(documentNumber)
                .filter(o -> o.getEmbassyId() != null && o.getEmbassyId().equals(embassyId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.DOCUMENT_NOT_FOUND, "error.document_not_found", HttpStatus.NOT_FOUND));
    }

    private String ensureVerificationCode(DocumentOrder order) {
        if (order.getVerificationCode() != null) {
            return order.getVerificationCode();
        }

        String code;
        do {
            code = randomCode();
        } while (orderRepository.existsByVerificationCode(code));

        order.setVerificationCode(code);
        return code;
    }

    private static String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private String buildHtml(DocumentOrder order, String verificationCode) {
        String title = text(order, "doc.title");
        String customerLabel = text(order, "doc.customer");
        String embassyLabel = text(order, "doc.embassy");
        String dateLabel = text(order, "doc.date");
        String statusLabel = text(order, "doc.status");
        String validLabel = text(order, "doc.valid");
        String totalLabel = text(order, "doc.total");
        String feeLabel = text(order, "doc.fee");
        String commissionLabel = text(order, "doc.commission");
        String copiesLabel = text(order, "doc.copies");
        String oneUnitLabel = text(order, "doc.oneUnit");
        String codeLabel = text(order, "doc.code");
        String noLabel = text(order, "doc.no");
        String qrLabel = text(order, "doc.qr");
        String signLabel = text(order, "doc.sign");

        String customerName = order.getCustomerId() == null
                ? ""
                : customerService.findById(order.getCustomerId())
                        .map(CustomerService.CustomerInfo::fullName)
                        .orElse("");
        String embassyName = order.getEmbassyId() == null
                ? ""
                : embassyService.findName(order.getEmbassyId()).orElse("");

        Map<Long, AccountResponse> accountsById = order.getCustomerId() == null
                ? Map.of()
                : customerService.accountsByIds(order.getCustomerId(), itemAccountIds(order)).stream()
                        .collect(Collectors.toMap(AccountResponse::id, Function.identity()));

        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            AccountResponse account = accountsById.get(item.getAccountId());
            String accountNumber = account == null ? "—" : account.accountNumber();
            String currency = account == null ? "" : account.currency().name();
            rows.append("<tr>")
                    .append("<td>").append(escape(accountNumber)).append("</td>")
                    .append("<td>").append(escape(currency)).append("</td>")
                    .append("<td>").append(escape(item.getPeriod().getCode())).append("</td>")
                    .append("<td>").append(escape(item.getStatementType().name())).append("</td>")
                    .append("<td>").append(item.isEquivalentCurrency() ? text(order, "doc.yes") : text(order, "doc.noValue"))
                    .append("</td>")
                    .append("</tr>");
        }

        String head = "<th>" + escape(text(order, "doc.account")) + "</th><th>" + escape(text(order, "doc.currency"))
                + "</th><th>" + escape(text(order, "doc.period")) + "</th><th>" + escape(text(order, "doc.type"))
                + "</th><th>" + escape(text(order, "doc.equivalent")) + "</th>";

        String price = order.getDocumentType().getPrice() + " " + order.getDocumentType().getCurrency();
        String date = order.getCreatedAt() == null ? "" : DATE_FORMAT.format(order.getCreatedAt());

        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:720px;margin:0 auto;color:#1a1a1a;\">"
                + "<div style=\"background:#e30613;color:#fff;padding:18px 28px;border-radius:8px 8px 0 0;"
                + "display:flex;justify-content:space-between;align-items:center;\">"
                + "<div><div style=\"font-size:26px;font-weight:700;letter-spacing:1px;\">ABB</div>"
                + "<div style=\"font-size:12px;opacity:.85;\">"
                + escape(text(order, "doc.abbService")) + "</div></div>"
                + "<div style=\"text-align:right;\"><div style=\"font-size:18px;font-weight:600;\">"
                + escape(title) + "</div><div style=\"font-size:12px;opacity:.85;\">"
                + escape(noLabel) + ": " + escape(order.getOrderNumber()) + "</div></div>"
                + "</div>"
                + "<div style=\"border:1px solid #e0e0e0;border-top:none;padding:28px;border-radius:0 0 8px 8px;"
                + "background:#fff;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:18px;\">"
                + "<tr><td style=\"padding:4px 0;\"><b>" + escape(noLabel) + ":</b> "
                + escape(order.getOrderNumber()) + "</td><td style=\"padding:4px 0;text-align:right;\"><b>"
                + escape(dateLabel) + ":</b> " + escape(date) + "</td></tr>"
                + "<tr><td style=\"padding:4px 0;\"><b>" + escape(customerLabel) + ":</b> "
                + escape(customerName) + "</td><td style=\"padding:4px 0;text-align:right;\"><b>"
                + escape(statusLabel) + ":</b> <span style=\"color:#19a260;\">" + escape(validLabel)
                + "</span></td></tr>"
                + (embassyName.isEmpty() ? "" : "<tr><td style=\"padding:4px 0;\"><b>" + escape(embassyLabel)
                + ":</b> " + escape(embassyName) + "</td><td></td></tr>")
                + "</table>"
                + "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"8\" border=\"1\" "
                + "style=\"border-collapse:collapse;border-color:#dde3ec;font-size:13px;\">"
                + "<thead><tr style=\"background:#f6f8ff;\">" + head + "</tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"6\" border=\"1\" "
                + "style=\"border-collapse:collapse;border-color:#dde3ec;font-size:13px;margin-top:16px;\">"
                + "<tr><td>" + escape(copiesLabel) + "</td><td>" + escape(oneUnitLabel) + "</td></tr>"
                + "<tr><td>" + escape(feeLabel) + "</td><td>" + escape(price) + "</td></tr>"
                + "<tr><td>" + escape(commissionLabel) + "</td><td>0.00 " + order.getDocumentType().getCurrency()
                + "</td></tr>"
                + "<tr style=\"font-weight:700;background:#f6f8ff;\"><td>" + escape(totalLabel)
                + "</td><td>" + escape(price) + "</td></tr>"
                + "</table>"
                + "<div style=\"display:flex;justify-content:space-between;align-items:center;"
                + "margin-top:22px;padding-top:18px;border-top:2px solid #e30613;\">"
                + "<div style=\"font-size:11px;color:#444;\">" + escape(qrLabel) + "</div>"
                + "<div style=\"text-align:center;\">"
                + "<div style=\"font-size:12px;color:#555;margin-bottom:6px;\">" + escape(codeLabel)
                + "</div><div style=\"font-weight:700;letter-spacing:2px;font-size:18px;color:#e30613;\">"
                + escape(verificationCode) + "</div></div>"
                + "</div>"
                + "<div style=\"text-align:right;margin-top:26px;font-size:12px;color:#555;\">"
                + "... ...<div style=\"margin-top:4px;\">" + escape(signLabel) + " | ABB</div></div>"
                + "</div></div>";
    }

    private String text(DocumentOrder order, String key) {
        Locale locale = order.getLanguage() == Language.EN ? Locale.ENGLISH : Locale.forLanguageTag("az");
        return messageSource.getMessage(key, null, locale);
    }

    private static List<Long> itemAccountIds(DocumentOrder order) {
        return order.getItems().stream().map(OrderItem::getAccountId).toList();
    }

    private static String toQrDataUri(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "error.generic",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
