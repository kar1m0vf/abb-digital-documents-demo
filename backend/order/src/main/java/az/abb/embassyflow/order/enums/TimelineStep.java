package az.abb.embassyflow.order.enums;

public enum TimelineStep {

    ORDER_RECEIVED("Sifariş qəbul edildi", "Order received"),
    OTP_VERIFIED("Şəxsiyyət təsdiqləndi", "Identity verified"),
    PAYMENT_RECEIVED("Ödəniş qəbul edildi", "Payment received"),
    ABB_APPROVED("ABB tərəfindən təsdiqləndi", "Approved by ABB"),
    DIGITALLY_SIGNED("Rəqəmsal imzalandı", "Digitally signed"),
    DELIVERED_TO_EMBASSY("Səfirliyə göndərildi", "Delivered to embassy"),
    EMBASSY_REVIEWED("Səfirlik tərəfindən yoxlanıldı", "Reviewed by embassy");

    private final String descriptionAz;
    private final String descriptionEn;

    TimelineStep(String descriptionAz, String descriptionEn) {
        this.descriptionAz = descriptionAz;
        this.descriptionEn = descriptionEn;
    }

    public String description(Language language) {
        return language == Language.AZ ? descriptionAz : descriptionEn;
    }
}
