package az.abb.embassyflow.order.enums;

public enum OrderStatus {
    CREATED,
    OTP_VERIFIED,
    PAYMENT_RECEIVED,
    PROCESSING,
    SIGNED,
    DELIVERED,
    COMPLETED,
    REJECTED
}