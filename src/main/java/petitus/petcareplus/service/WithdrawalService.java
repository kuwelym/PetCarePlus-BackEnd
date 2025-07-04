package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.configuration.WalletConfig;
import petitus.petcareplus.dto.request.wallet.WithdrawalRequest;
import petitus.petcareplus.dto.response.wallet.WithdrawalResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.model.wallet.Withdrawal;
import petitus.petcareplus.repository.WithdrawalRepository;
import petitus.petcareplus.utils.enums.TransactionStatus;
import petitus.petcareplus.utils.enums.TransactionType;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final WalletConfig walletConfig;

    @Transactional
    public WithdrawalResponse createWithdrawalRequest(UUID providerId, WithdrawalRequest request) {

        // 1. Validate provider
        User provider = userService.getUser();
        log.info("Provider role: {}", provider.getRole().getName());
        if (!provider.getRole().getName().toString().equals("SERVICE_PROVIDER")) {
            throw new BadRequestException("Only service providers can request withdrawals");
        }

        // 2. Get provider wallet
        Wallet wallet = walletService.getWalletByUserId(providerId);

        // 3. Calculate fees
        BigDecimal fee = calculateWithdrawalFee(request.getAmount());
        BigDecimal netAmount = request.getAmount().subtract(fee);

        // 4. Validate balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient balance for withdrawal");
        }

        // 5. Check daily/monthly limits
        validateWithdrawalLimits(providerId, request.getAmount());

        try {
            // 6. Create withdrawal record
            Withdrawal withdrawal = Withdrawal.builder()
                    .wallet(wallet)
                    .provider(provider)
                    .amount(request.getAmount())
                    .fee(fee)
                    .netAmount(netAmount)
                    .status(WithdrawalStatus.PENDING)
                    .bankCode(request.getBankCode())
                    .bankName(request.getBankName())
                    .accountNumber(request.getAccountNumber())
                    .accountHolderName(request.getAccountHolderName())
                    .build();

            withdrawal = withdrawalRepository.save(withdrawal);

            // 7. Hold the amount in wallet (move from balance to pending)
            wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
            wallet.setPendingBalance(wallet.getPendingBalance().add(request.getAmount()));
            walletService.updateWallet(wallet);

            // 8. Create wallet transaction
            walletService.createWalletTransaction(
                    providerId,
                    request.getAmount().negate(), // Negative amount for withdrawal
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.PENDING,
                    "Withdrawal request: " + withdrawal.getId(),
                    null);

            // 9. Send notification
            // notificationService.sendWithdrawalRequestNotification(provider, withdrawal);

            log.info("Withdrawal request created: {} for provider: {}", withdrawal.getId(), providerId);

            return mapToWithdrawalResponse(withdrawal);
        } catch (Exception e) {
            log.error("Error creating withdrawal request: {}", e.getMessage());
            throw new BadRequestException("Failed to create withdrawal request: " + e.getMessage());
        }

    }

    public Page<WithdrawalResponse> getProviderWithdrawals(UUID providerId, Pageable pageable) {
        return withdrawalRepository.findByProviderIdOrderByCreatedAtDesc(providerId, pageable)
                .map(this::mapToWithdrawalResponse);
    }

    public Page<WithdrawalResponse> getAllWithdrawals(Pageable pageable) {
        return withdrawalRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToWithdrawalResponse);
    }

    @Transactional
    public WithdrawalResponse approveWithdrawal(UUID withdrawalId, String adminNote) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only pending withdrawals can be approved");
        }

        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setAdminNote(adminNote);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setProcessedBy(userService.getUser().getFullName());

        withdrawal = withdrawalRepository.save(withdrawal);

        return mapToWithdrawalResponse(withdrawal);
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(UUID withdrawalId, String rejectionReason) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only pending withdrawals can be rejected");
        }

        // Return money to wallet
        Wallet wallet = withdrawal.getWallet();
        wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
        wallet.setPendingBalance(wallet.getPendingBalance().subtract(withdrawal.getAmount()));
        walletService.updateWallet(wallet);

        // Update withdrawal status
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(rejectionReason);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setProcessedBy(userService.getUser().getFullName());

        withdrawal = withdrawalRepository.save(withdrawal);

        // Update wallet transaction
        walletService.createWalletTransaction(
                withdrawal.getProvider().getId(),
                withdrawal.getAmount(), // Positive amount (refund)
                TransactionType.SYSTEM_ADJUSTMENT,
                TransactionStatus.COMPLETED,
                "Withdrawal rejected: " + withdrawal.getId(),
                null);

        return mapToWithdrawalResponse(withdrawal);
    }

    public BigDecimal calculateWithdrawalFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(walletConfig.getFeeRate());

        if (fee.compareTo(walletConfig.getMinFee()) < 0) {
            return walletConfig.getMinFee();
        }

        if (fee.compareTo(walletConfig.getMaxFee()) > 0) {
            return walletConfig.getMaxFee();
        }

        return fee;
    }

    private void validateWithdrawalLimits(UUID providerId, BigDecimal amount) {
        // Daily limit: 10,000,000 VND
        BigDecimal dailyLimit = new BigDecimal("10000000");
        BigDecimal todayTotal = withdrawalRepository.getTodayWithdrawalTotal(providerId);

        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new BadRequestException("Daily withdrawal limit exceeded");
        }

        // Monthly limit: 100,000,000 VND
        BigDecimal monthlyLimit = new BigDecimal("100000000");
        BigDecimal monthTotal = withdrawalRepository.getMonthWithdrawalTotal(providerId);

        if (monthTotal.add(amount).compareTo(monthlyLimit) > 0) {
            throw new BadRequestException("Monthly withdrawal limit exceeded");
        }
    }

    @Transactional
    public WithdrawalResponse completeWithdrawal(UUID withdrawalId, String transactionNote) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setAdminNote(transactionNote);
        withdrawal.setTransactionRef("TXN" + System.currentTimeMillis());

        // Remove from pending balance
        Wallet wallet = withdrawal.getWallet();
        wallet.setPendingBalance(wallet.getPendingBalance().subtract(withdrawal.getAmount()));
        walletService.updateWallet(wallet);

        // Update wallet transaction
        walletService.createWalletTransaction(
                withdrawal.getProvider().getId(),
                withdrawal.getAmount().negate(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.COMPLETED,
                "Withdrawal completed: " + withdrawal.getId(),
                null);

        withdrawal = withdrawalRepository.save(withdrawal);

        // Send success notification
        // notificationService.sendWithdrawalCompletedNotification(withdrawal.getProvider(),
        // withdrawal);

        log.info("Withdrawal completed: {}", withdrawalId);

        return mapToWithdrawalResponse(withdrawal);
    }

    private Withdrawal getWithdrawalById(UUID withdrawalId) {
        return withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found: " + withdrawalId));
    }

    private WithdrawalResponse mapToWithdrawalResponse(Withdrawal withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .fee(withdrawal.getFee())
                .netAmount(withdrawal.getNetAmount())
                .status(withdrawal.getStatus())
                .bankName(withdrawal.getBankName())
                .accountNumber(withdrawal.getAccountNumber())
                .accountHolderName(withdrawal.getAccountHolderName())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .adminNote(withdrawal.getAdminNote())
                .rejectionReason(withdrawal.getRejectionReason())
                .transactionRef(withdrawal.getTransactionRef())
                .build();
    }

    // private String maskAccountNumber(String accountNumber) {
    // if (accountNumber.length() <= 4) {
    // return accountNumber;
    // }
    // return "*".repeat(accountNumber.length() - 4) +
    // accountNumber.substring(accountNumber.length() - 4);
    // }
}