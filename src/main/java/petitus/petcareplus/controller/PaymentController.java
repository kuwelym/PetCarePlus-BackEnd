package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

import java.util.List;
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

    // @GetMapping("/vnpay-return")
    // @Operation(summary = "VNPAY Return URL", description = "Handle the return
    // from VNPAY payment gateway and redicect to the frontend")
    // public String vnpayReturn(Model model, HttpServletRequest request) {

    // org.apache.commons.logging.LogFactory.getLog(PaymentController.class)
    // .info("Raw: " + request.getQueryString());

    // Map<String, String> params =
    // VnpayUtils.extractRawVnpParams(request.getQueryString());

    // // Log the parameters for debugging
    // org.apache.commons.logging.LogFactory.getLog(PaymentController.class)
    // .info("VNPAY Return Parameters: " + params);

    // PaymentResponse response = paymentService.processVnpayReturn(params);

    // model.addAttribute("status", response.getStatus());
    // model.addAttribute("orderCode", response.getTransactionCode());
    // model.addAttribute("amount", response.getAmount());
    // model.addAttribute("bankCode", response.getBankCode());

    // return "payment-result";
    // }

    // Get payment by transaction code
    // @GetMapping("/transaction/{transactionCode}")
    // @PreAuthorize("isAuthenticated()")
    // @Operation(summary = "Get payment by transaction code", description = "Get
    // payment details by transaction code")
    // public ResponseEntity<PaymentResponse> getPaymentByTransactionCode(
    // @AuthenticationPrincipal JwtUserDetails userDetails,
    // @PathVariable String transactionCode) {
    // PaymentResponse payment =
    // paymentService.getPaymentByTransactionCode(userDetails.getId(),
    // transactionCode);
    // return ResponseEntity.ok(payment);
    // }

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