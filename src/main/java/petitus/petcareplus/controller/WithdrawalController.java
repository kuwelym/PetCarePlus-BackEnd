package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.wallet.WithdrawalRequest;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.wallet.WithdrawalResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.WithdrawalService;

@RestController
@RequestMapping("/withdrawals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Withdrawals", description = "APIs for managing withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping
    @Operation(summary = "Create withdrawal request", description = "Create a new withdrawal request for service provider")
    public ResponseEntity<WithdrawalResponse> createWithdrawalRequest(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request) {

        WithdrawalResponse response = withdrawalService.createWithdrawalRequest(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my withdrawals", description = "Get withdrawal history for current provider")
    public ResponseEntity<PaginationResponse<WithdrawalResponse>> getMyWithdrawals(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<WithdrawalResponse> withdrawals = withdrawalService.getProviderWithdrawals(userDetails.getId(),
                pageRequest);

        PaginationResponse<WithdrawalResponse> response = new PaginationResponse<>(
                withdrawals,
                withdrawals.getContent());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/fee-calculator")
    @Operation(summary = "Calculate withdrawal fee", description = "Calculate fee for withdrawal amount")
    public ResponseEntity<Map<String, Object>> calculateFee(@RequestParam BigDecimal amount) {
        // This would call the fee calculation logic
        BigDecimal fee = withdrawalService.calculateWithdrawalFee(amount);
        BigDecimal netAmount = amount.subtract(fee);

        Map<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("fee", fee);
        result.put("netAmount", netAmount);
        result.put("feeRate", "1%");
        result.put("minFee", "5,000 VND");
        result.put("maxFee", "50,000 VND");

        return ResponseEntity.ok(result);
    }
}