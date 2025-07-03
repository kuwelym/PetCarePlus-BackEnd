package petitus.petcareplus.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.wallet.WithdrawalResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.service.WithdrawalService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/withdrawals")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Withdrawals", description = "Admin APIs for managing withdrawals")
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping
    @Operation(summary = "Get all withdrawals", description = "Get all withdrawal requests with pagination")
    public ResponseEntity<PaginationResponse<WithdrawalResponse>> getAllWithdrawals(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<WithdrawalResponse> withdrawals = withdrawalService.getAllWithdrawals(pageRequest);
        PaginationResponse<WithdrawalResponse> response = new PaginationResponse<>(
                withdrawals,
                withdrawals.getContent());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{withdrawalId}/approve")
    @Operation(summary = "Approve withdrawal", description = "Approve a pending withdrawal request")
    public ResponseEntity<WithdrawalResponse> approveWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestBody Map<String, String> request) {

        String adminNote = request.getOrDefault("adminNote", "Approved by admin");
        WithdrawalResponse response = withdrawalService.approveWithdrawal(withdrawalId, adminNote);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{withdrawalId}/reject")
    @Operation(summary = "Reject withdrawal", description = "Reject a pending withdrawal request")
    public ResponseEntity<WithdrawalResponse> rejectWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestBody Map<String, String> request) {

        String rejectionReason = request.get("rejectionReason");
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required");
        }

        WithdrawalResponse response = withdrawalService.rejectWithdrawal(withdrawalId, rejectionReason);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{withdrawalId}/complete")
    @Operation(summary = "Complete withdrawal", description = "Mark withdrawal as completed after bank transfer")
    public ResponseEntity<WithdrawalResponse> completeWithdrawal(
            @PathVariable UUID withdrawalId,
            @RequestBody Map<String, String> request) {

        String transactionNote = request.getOrDefault("transactionNote", "Bank transfer completed");
        WithdrawalResponse response = withdrawalService.completeWithdrawal(withdrawalId, transactionNote);

        return ResponseEntity.ok(response);
    }
}