package petitus.petcareplus.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.payment.PaymentResponse;
import petitus.petcareplus.service.PaymentService;
import petitus.petcareplus.utils.VnpayUtils;
import petitus.petcareplus.utils.enums.PaymentStatus;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "APIs for managing page payments ")
public class PaymentPageController {

        private final PaymentService paymentService;

        @GetMapping("/vnpay-return")
        @Operation(summary = "VNPAY Return URL", description = "Handle the return from VNPAY payment gateway and redicect to the frontend")
        public String vnpayReturn(Model model, HttpServletRequest request) {

                Map<String, String> params = VnpayUtils.extractRawVnpParams(request.getQueryString());

                PaymentResponse response = paymentService.processVnpayReturn(params);

                model.addAttribute("status", response.getStatus().toString());
                model.addAttribute("orderCode", response.getTransactionCode());
                model.addAttribute("amount", response.getAmount());
                model.addAttribute("bankCode", response.getBankCode());
                model.addAttribute("message", response.getStatus().compareTo(PaymentStatus.COMPLETED) == 0
                                ? "Giao dịch thành công"
                                : "Giao dịch thất bại");

                return "payment-result";
        }

}
