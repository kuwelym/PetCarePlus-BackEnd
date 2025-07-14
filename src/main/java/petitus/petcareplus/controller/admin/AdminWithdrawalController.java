package petitus.petcareplus.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.wallet.WithdrawalResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.WithdrawalCriteria;
import petitus.petcareplus.service.WithdrawalService;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
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
    public ResponseEntity<StandardPaginationResponse<WithdrawalResponse>> getAllWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) Boolean isDeleted,

            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "createdAt", "updatedAt", "amount", "netAmount", "transactionRef" }) // Allowed
                // sort
                // fields
                .build();

        WithdrawalCriteria criteria = WithdrawalCriteria.builder()
                .status(status)
                .amountFrom(amountFrom)
                .amountTo(amountTo)
                .bankName(bankName)
                .isDeleted(isDeleted)
                .build();

        // Lấy Page từ service
        Page<WithdrawalResponse> pageResult = withdrawalService.getAllWithdrawals(pagination, criteria);

        // Convert sang PaginationResponse
        StandardPaginationResponse<WithdrawalResponse> response = new StandardPaginationResponse<>(
                pageResult,
                pageResult.getContent());

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