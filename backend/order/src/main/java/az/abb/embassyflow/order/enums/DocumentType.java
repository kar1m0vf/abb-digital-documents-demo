package az.abb.embassyflow.order.enums;

import java.math.BigDecimal;

public enum DocumentType {

    ACCOUNT_STATEMENT(
            "Hesabdan Çıxarış",
            "Account Statement",
            "Seçdiyiniz hesabdan dövri çıxarış",
            "Periodic statement from your chosen account",
            new BigDecimal("5.00")),
    EMBASSY_CERTIFICATE(
            "Səfirliyə arayış",
            "Reference Letter",
            "Səfirliklər üçün standart sertifikat",
            "Standard certificate for embassies",
            new BigDecimal("10.00"));

    private final String nameAz;
    private final String nameEn;
    private final String descriptionAz;
    private final String descriptionEn;
    private final BigDecimal price;
    private final String currency;

    DocumentType(String nameAz, String nameEn, String descriptionAz, String descriptionEn, BigDecimal price) {
        this.nameAz = nameAz;
        this.nameEn = nameEn;
        this.descriptionAz = descriptionAz;
        this.descriptionEn = descriptionEn;
        this.price = price;
        this.currency = "AZN";
    }

    public String getNameAz() {
        return nameAz;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescriptionAz() {
        return descriptionAz;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}