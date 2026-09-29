package az.abb.embassyflow.common.web;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = System.getLogger("az.abb.embassyflow.common.web.GlobalExceptionHandler");

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getHttpStatus(), ex.getCode(), ex.getMessageKey(), ex.getArgs(), request);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "error.validation", new Object[0], request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.log(Level.ERROR, "Unexpected error on " + request.getMethod() + " " + request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "error.generic", new Object[0], request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String messageKey, Object[] args,
                                           HttpServletRequest request) {
        String message = messageSource.getMessage(messageKey, args, LocaleContextHolder.getLocale());
        ApiError body = new ApiError(status.value(), code, message, Instant.now(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}