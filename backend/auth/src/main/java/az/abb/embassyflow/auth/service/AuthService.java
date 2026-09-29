package az.abb.embassyflow.auth.service;

import az.abb.embassyflow.auth.dao.entity.CustomerOtp;
import az.abb.embassyflow.auth.dao.repository.CustomerOtpRepository;
import az.abb.embassyflow.auth.dto.request.FinVerifyRequest;
import az.abb.embassyflow.auth.dto.request.OtpSendRequest;
import az.abb.embassyflow.auth.dto.request.OtpValidateRequest;
import az.abb.embassyflow.auth.dto.response.FinVerifyResponse;
import az.abb.embassyflow.auth.dto.response.OtpSendResponse;
import az.abb.embassyflow.auth.dto.response.OtpValidateResponse;
import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.customer.service.CustomerService.CustomerInfo;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_SECONDS = 63;
    private static final int TOKEN_TTL_SECONDS = 3600;
    private static final String TOKEN_TYPE = "Bearer";

    private final CustomerService customerService;
    private final CustomerOtpRepository otpRepository;
    private final String configuredDemoOtp;

    public AuthService(CustomerService customerService, CustomerOtpRepository otpRepository,
                       @Value("${demo.otp:}") String configuredDemoOtp) {
        this.customerService = customerService;
        this.otpRepository = otpRepository;
        this.configuredDemoOtp = configuredDemoOtp;
    }

    @Transactional(readOnly = true)
    public FinVerifyResponse verifyFin(FinVerifyRequest request) {
        CustomerInfo customer = customerService.findByFin(request.fin())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.CUSTOMER_NOT_FOUND, "error.customer_not_found", HttpStatus.NOT_FOUND));

        return new FinVerifyResponse(customer.id(), customer.fullName(), maskPhone(customer.phone()));
    }

    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        CustomerInfo customer = customerService.findById(request.customerId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.CUSTOMER_NOT_FOUND, "error.customer_not_found", HttpStatus.NOT_FOUND));

        String code = generateCode();

        otpRepository.deleteByCustomerId(customer.id());
        CustomerOtp otp = new CustomerOtp();
        otp.setCustomerId(customer.id());
        otp.setCode(code);
        otp.setExpiresAt(Instant.now().plusSeconds(OTP_TTL_SECONDS));
        otp.setVerified(false);
        otpRepository.save(otp);

        return new OtpSendResponse(maskPhone(customer.phone()), OTP_TTL_SECONDS, code);
    }

    @Transactional
    public OtpValidateResponse validateOtp(OtpValidateRequest request) {
        CustomerOtp otp = otpRepository.findFirstByCustomerIdOrderByIdDesc(request.customerId())
                .orElseThrow(this::invalidOtp);

        if (otp.isVerified() || otp.getExpiresAt().isBefore(Instant.now()) || !otp.getCode().equals(request.otp())) {
            throw invalidOtp();
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        String token = "demo-token-customer-" + otp.getCustomerId();
        return new OtpValidateResponse(token, TOKEN_TYPE, TOKEN_TTL_SECONDS);
    }

    private BusinessException invalidOtp() {
        return new BusinessException(ErrorCodes.INVALID_OTP, "error.invalid_otp", HttpStatus.BAD_REQUEST);
    }

    private String generateCode() {
        if (configuredDemoOtp != null && !configuredDemoOtp.isBlank()) {
            return configuredDemoOtp;
        }
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 12) {
            return phone;
        }
        String operator = digits.substring(3, 5);
        String lastTwo = digits.substring(digits.length() - 2);
        return "+" + digits.substring(0, 3) + " " + operator + " *** ** " + lastTwo;
    }
}