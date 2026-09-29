package az.abb.embassyflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerOtpRepository otpRepository;

    @InjectMocks
    private AuthService authService;

    private static final CustomerInfo CUSTOMER = new CustomerInfo(42L, "Aydan Ahadova", "+994503304582");

    @Test
    void verifyFin_knownFin_returnsMaskedPhone() {
        when(customerService.findByFin("5D7X9Q2")).thenReturn(Optional.of(CUSTOMER));

        FinVerifyResponse response = authService.verifyFin(new FinVerifyRequest("5D7X9Q2"));

        assertEquals(42L, response.customerId());
        assertEquals("Aydan Ahadova", response.fullName());
        assertEquals("+994 50 *** ** 82", response.phoneMasked());
    }

    @Test
    void verifyFin_unknownFin_throwsCustomerNotFound() {
        when(customerService.findByFin("XXXXXXX")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyFin(new FinVerifyRequest("XXXXXXX")));

        assertEquals(ErrorCodes.CUSTOMER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void sendOtp_knownCustomer_persistsOtpAndReturnsDemoCode() {
        when(customerService.findById(42L)).thenReturn(Optional.of(CUSTOMER));
        when(otpRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OtpSendResponse response = authService.sendOtp(new OtpSendRequest(42L));

        verify(otpRepository).deleteByCustomerId(42L);
        ArgumentCaptor<CustomerOtp> captor = ArgumentCaptor.forClass(CustomerOtp.class);
        verify(otpRepository).save(captor.capture());

        CustomerOtp saved = captor.getValue();
        assertEquals(42L, saved.getCustomerId());
        assertEquals(6, saved.getCode().length());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
        assertEquals("+994 50 *** ** 82", response.sentTo());
        assertEquals(63, response.expiresInSeconds());
        assertEquals(saved.getCode(), response.demoOtp());
    }

    @Test
    void sendOtp_unknownCustomer_throwsCustomerNotFound() {
        when(customerService.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendOtp(new OtpSendRequest(99L)));

        assertEquals(ErrorCodes.CUSTOMER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void validateOtp_correctCode_returnsToken() {
        CustomerOtp otp = otp("123456", Instant.now().plusSeconds(30), false);
        when(otpRepository.findFirstByCustomerIdOrderByIdDesc(42L)).thenReturn(Optional.of(otp));
        when(otpRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OtpValidateResponse response = authService.validateOtp(new OtpValidateRequest(42L, "123456"));

        assertEquals("demo-token-customer-42", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertTrue(otp.isVerified());
    }

    @Test
    void validateOtp_wrongCode_throwsInvalidOtp() {
        CustomerOtp otp = otp("123456", Instant.now().plusSeconds(30), false);
        when(otpRepository.findFirstByCustomerIdOrderByIdDesc(42L)).thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.validateOtp(new OtpValidateRequest(42L, "999999")));

        assertEquals(ErrorCodes.INVALID_OTP, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void validateOtp_expiredCode_throwsInvalidOtp() {
        CustomerOtp otp = otp("123456", Instant.now().minusSeconds(1), false);
        when(otpRepository.findFirstByCustomerIdOrderByIdDesc(42L)).thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.validateOtp(new OtpValidateRequest(42L, "123456")));

        assertEquals(ErrorCodes.INVALID_OTP, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void validateOtp_noOtp_throwsInvalidOtp() {
        when(otpRepository.findFirstByCustomerIdOrderByIdDesc(42L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.validateOtp(new OtpValidateRequest(42L, "123456")));

        assertEquals(ErrorCodes.INVALID_OTP, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    private CustomerOtp otp(String code, Instant expiresAt, boolean verified) {
        CustomerOtp otp = new CustomerOtp();
        otp.setCustomerId(42L);
        otp.setCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setVerified(verified);
        return otp;
    }
}