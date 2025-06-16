package petitus.petcareplus.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import petitus.petcareplus.dto.response.payment.VnpayReturnResponse;
import petitus.petcareplus.service.PayOSPaymentService;
import petitus.petcareplus.service.PaymentService;
import petitus.petcareplus.utils.ParamsUtils;

@Slf4j
@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Page Payment", description = "APIs for managing page payments ")
public class PaymentPageController {

        private final PaymentService paymentService;
        private final PayOSPaymentService payOSPaymentService;

        @GetMapping("/vnpay-return")
        @Operation(summary = "VNPAY Return URL", description = "Handle the return from VNPAY payment gateway and redicect to the frontend")
        public String vnpayReturn(Model model, HttpServletRequest request) {

                Map<String, String> params = ParamsUtils.extractRawParams(request.getQueryString());

                VnpayReturnResponse response = paymentService.verifyVnpayReturn(params);

                model.addAttribute("message", response.getMessage());

                switch (response.getStatus()) {
                        case SUCCESS:
                        case FAIL:
                                model.addAttribute("amount", response.getAmount());
                                model.addAttribute("bankCode", response.getBankCode());
                                model.addAttribute("cardType", response.getCardType());
                                String decodedOrderInfo = URLDecoder.decode(response.getOrderInfo(),
                                                StandardCharsets.UTF_8);
                                model.addAttribute("orderInfo", decodedOrderInfo);
                                model.addAttribute("payDate", response.getPayDate());
                                model.addAttribute("responseCode", response.getResponseCode());

                                break;
                        case INVALID_SIGNATURE:
                                break;
                        default:
                                return "error";
                }

                return switch (response.getStatus()) {
                        case SUCCESS -> "payment-success";
                        case FAIL -> "payment-fail";
                        case INVALID_SIGNATURE -> "sign-invalid";
                        default -> "error";
                };

        }

        @GetMapping("/payos-return")
        @Operation(summary = "Payos Return URL", description = "Handle the return from Payos payment gateway and redicect to the frontend")
        public String payosReturn(Model model, HttpServletRequest request) {

                log.info("START PayOS RETURN HANDLER");

                Map<String, String> params = ParamsUtils.extractRawParams(request.getQueryString());

                model.addAttribute("amount", params.get("amount"));

                log.info("END PayOS RETURN HANDLER");
                return "payment-success";
        };

        @GetMapping("/payos-cancel")
        @Operation(summary = "Payos Cancel URL", description = "Handle the cancel from Payos payment gateway and redicect to the frontend")
        public String payosCancel(Model model, HttpServletRequest request) {

                Map<String, String> params = ParamsUtils.extractRawParams(request.getQueryString());

                String orderCode = params.get("orderCode");

                // check payment status after 3 seconds delay
                if (orderCode != null) {
                        checkPaymentStatusAfterDelay(orderCode, 3);
                }

                model.addAttribute("message", "Payment cancelled by user");
                model.addAttribute("amount", params.get("amount"));
                model.addAttribute("orderCode", params.get("orderCode"));
                model.addAttribute("status", params.get("status"));

                return "payment-fail";
        }

        @Async
        private void checkPaymentStatusAfterDelay(String orderCode, int delayInSeconds) {
                CompletableFuture.delayedExecutor(delayInSeconds, TimeUnit.SECONDS)
                                .execute(() -> {
                                        try {
                                                log.info("Checking PayOS payment status for order code: {} after {} seconds delay",
                                                                orderCode, delayInSeconds);

                                                payOSPaymentService.checkAndUpdatePaymentStatus(orderCode);

                                        } catch (Exception e) {
                                                log.error("Error checking PayOS payment status for order code: {}",
                                                                orderCode, e);
                                        }
                                });
        }
}
