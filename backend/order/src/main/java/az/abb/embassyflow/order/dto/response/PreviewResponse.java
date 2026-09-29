package az.abb.embassyflow.order.dto.response;

public record PreviewResponse(
        String documentNumber,
        String htmlPreview,
        String qrCodeBase64,
        String verificationCode) {
}
