package az.abb.embassyflow.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Period {

    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y");

    private final String code;

    Period(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Period fromCode(String value) {
        for (Period period : values()) {
            if (period.code.equals(value)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unknown period: " + value);
    }
}
