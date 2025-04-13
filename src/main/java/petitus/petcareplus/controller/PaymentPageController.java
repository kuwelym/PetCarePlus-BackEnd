package petitus.petcareplus.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.payment.VnpayReturnResponse;
import petitus.petcareplus.service.PaymentService;
import petitus.petcareplus.utils.VnpayUtils;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Page Payment", description = "APIs for managing page payments ")
public class PaymentPageController {

        private final PaymentService paymentService;

        @GetMapping("/vnpay-return")
        @Operation(summary = "VNPAY Return URL", description = "Handle the return from VNPAY payment gateway and redicect to the frontend")
        public String vnpayReturn(Model model, HttpServletRequest request) {

                Map<String, String> params = VnpayUtils.extractRawVnpParams(request.getQueryString());

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

}
