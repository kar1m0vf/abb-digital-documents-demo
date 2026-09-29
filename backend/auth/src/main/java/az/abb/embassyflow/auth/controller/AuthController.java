package az.abb.embassyflow.auth.controller;

import az.abb.embassyflow.auth.dto.request.FinVerifyRequest;
import az.abb.embassyflow.auth.dto.request.OtpSendRequest;
import az.abb.embassyflow.auth.dto.request.OtpValidateRequest;
import az.abb.embassyflow.auth.dto.response.FinVerifyResponse;
import az.abb.embassyflow.auth.dto.response.OtpSendResponse;
import az.abb.embassyflow.auth.dto.response.OtpValidateResponse;
import az.abb.embassyflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Auth", description = "FİN/OTP demo müştəri autentifikasiyası")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "FİN yoxlama və demo hesabı ilə əlaqələndirmə")
    @PostMapping("/fin/verify")
    public FinVerifyResponse verifyFin(@Valid @RequestBody FinVerifyRequest request) {
        return authService.verifyFin(request);
    }

    @Operation(summary = "OTP göndər (demo: 123456)")
    @PostMapping("/otp/send")
    public OtpSendResponse sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return authService.sendOtp(request);
    }

    @Operation(summary = "OTP yoxlama və access token qaytarma")
    @PostMapping("/otp/validate")
    public OtpValidateResponse validateOtp(@Valid @RequestBody OtpValidateRequest request) {
        return authService.validateOtp(request);
    }
}