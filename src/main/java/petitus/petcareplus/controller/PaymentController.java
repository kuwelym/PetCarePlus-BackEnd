package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.request.payment.CreatePaymentRequest;
import petitus.petcareplus.dto.response.payment.PaymentResponse;
import petitus.petcareplus.dto.response.payment.PaymentUrlResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.PaymentService;
import petitus.petcareplus.utils.ParamsUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "APIs for managing payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-vnpay")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Create VNPAY payment URL", description = "Create a payment URL for VNPAY payment gateway")
    public ResponseEntity<PaymentUrlResponse> createVnpayPayment(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentUrlResponse response = paymentService.createVnPayUrl(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-ipn")
    @Operation(summary = "VNPAY IPN URL", description = "Handle the IPN from VNPAY payment gateway")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {

        Map<String, String> params = ParamsUtils.extractRawParams(request.getQueryString());

        ResponseEntity<Map<String, String>> response = paymentService.handleIPNUrl(params);
        return response;
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking payments", description = "Get all payments for a booking")
    public ResponseEntity<List<PaymentResponse>> getBookingPayments(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID bookingId) {
        List<PaymentResponse> payments = paymentService.getBookingPayments(userDetails.getId(), bookingId);
        return ResponseEntity.ok(payments);
    }
}