package az.abb.embassyflow.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final String messageKey;
    private final Object[] args;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String messageKey, HttpStatus httpStatus, Object... args) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Object[] getArgs() {
        return args;
    }
}