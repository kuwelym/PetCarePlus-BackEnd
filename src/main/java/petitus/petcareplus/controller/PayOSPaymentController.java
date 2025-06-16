package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import petitus.petcareplus.dto.request.payment.CreatePayOSPaymentRequest;
import petitus.petcareplus.dto.response.payment.PaymentUrlResponse;
import petitus.petcareplus.service.PayOSPaymentService;
import vn.payos.type.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments/payos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments PayOS", description = "APIs for managing payments using PayOS")
public class PayOSPaymentController {

    private final PayOSPaymentService payOSPaymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentUrlResponse> createPayOSPayment(
            @Valid @RequestBody CreatePayOSPaymentRequest request) {

        log.info("Creating PayOS payment for booking: {}, amount: {}",
                request.getBookingId(), request.getAmount());

        PaymentUrlResponse response = payOSPaymentService.createPayOSPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePayOSWebhook(
            @RequestBody Webhook webhookBody,
            @RequestHeader(value = "PayOS-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("Webhook headers: {}", Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, request::getHeader)));
        try {
            log.info("PayOS webhook received");

            payOSPaymentService.handlePayOSWebhook(webhookBody);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error handling PayOS webhook: ", e);
            return ResponseEntity.badRequest().body("Error");
        }
    }

    @PostMapping("/cancel/{orderCode}")
    public ResponseEntity<String> cancelPayment(
            @PathVariable String orderCode,
            @RequestParam(defaultValue = "User cancelled") String reason) {
        try {
            log.info("Cancelling PayOS payment. Order code: {}, Reason: {}", orderCode, reason);

            payOSPaymentService.cancelPaymentLink(orderCode, reason);
            return ResponseEntity.ok("Payment cancelled successfully");

        } catch (Exception e) {
            log.error("Error cancelling payment: ", e);
            return ResponseEntity.badRequest().body("Error cancelling payment: " + e.getMessage());
        }
    }

    @GetMapping("/info/{orderCode}")
    public ResponseEntity<?> getPaymentInfo(@PathVariable String orderCode) {
        try {
            log.info("Getting PayOS payment info for order code: {}", orderCode);

            return ResponseEntity.ok(payOSPaymentService.getPaymentLinkInfo(orderCode));

        } catch (Exception e) {
            log.error("Error getting payment info: ", e);
            return ResponseEntity.badRequest().body("Error getting payment info: " + e.getMessage());
        }
    }
}