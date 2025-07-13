package petitus.petcareplus.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.wallet.WalletResponse;
import petitus.petcareplus.dto.response.wallet.WalletTransactionResponse;
import petitus.petcareplus.exceptions.DataExistedException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Booking;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.model.wallet.WalletTransaction;
import petitus.petcareplus.repository.BookingRepository;
import petitus.petcareplus.repository.WalletRepository;
import petitus.petcareplus.repository.WalletTransactionRepository;
import petitus.petcareplus.utils.enums.TransactionStatus;
import petitus.petcareplus.utils.enums.TransactionType;

@Service
@RequiredArgsConstructor
public class WalletService {

        private final WalletRepository walletRepository;
        private final WalletTransactionRepository walletTransactionRepository;
        private final UserService userService;
        // private final PaymentRepository paymentRepository;
        private final BookingRepository bookingRepository;
        private final MessageSourceService messageSourceService;

        public Wallet getWalletByUserId(UUID userId) {
                return walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                messageSourceService.get("wallet_not_found")));
        }

        public WalletResponse getWalletByUser(UUID userId) {
                Wallet wallet = walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                messageSourceService.get("wallet_not_found")));
                return mapToWalletResponse(wallet);
        }

        @Transactional
        public WalletResponse createWallet(UUID userId) {

                // Only service providers can create wallets
                if (!userService.hasRole("SERVICE_PROVIDER")) {
                        throw new ResourceNotFoundException(
                                        messageSourceService.get("only_service_provider_can_create_wallet"));
                }

                // Check if wallet already exists for the user
                if (walletRepository.findByUserId(userId).isPresent()) {
                        throw new DataExistedException(messageSourceService.get("wallet_already_exists"));
                }

                Wallet wallet = Wallet.builder()
                                .user(userService.getUser())
                                .balance(BigDecimal.ZERO) // Initialize balance to 0
                                .pendingBalance(BigDecimal.ZERO) // Initialize pending balance to 0
                                .build();

                Wallet savedWallet = walletRepository.save(wallet);
                return mapToWalletResponse(savedWallet);
        }

        public Page<WalletTransactionResponse> getWalletTransactions(UUID userId, Pageable pageable) {

                // Check if wallet exists for the user

                Wallet wallet = walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                messageSourceService.get("wallet_not_found")));

                return walletTransactionRepository.findByWalletId(wallet.getId(), pageable)
                                .map(this::mapToWalletTransactionResponse);
        }

        @Transactional
        public WalletTransaction createWalletTransaction(UUID userId, BigDecimal amount, TransactionType type,
                        TransactionStatus status, String description, UUID bookingId) {

                // Check if wallet exists for the user
                Wallet wallet = walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                messageSourceService.get("wallet_not_found")));

                Booking booking = null;

                if (type == TransactionType.SERVICE_PROVIDER_EARNING) {
                        booking = bookingRepository.findById(bookingId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        messageSourceService.get("booking_not_found")));
                }

                WalletTransaction transaction = WalletTransaction.builder()
                                .wallet(wallet)
                                .amount(amount)
                                .type(type)
                                .booking(booking)
                                .status(status)
                                .description(description)
                                .build();

                walletTransactionRepository.save(transaction);

                return transaction;
        }

        // Update wallet
        @Transactional
        public void updateWallet(Wallet wallet) {
                // Check if wallet exists
                Wallet existingWallet = walletRepository.findById(wallet.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                messageSourceService.get("wallet_not_found")));

                // Update fields
                existingWallet.setBalance(wallet.getBalance());
                existingWallet.setPendingBalance(wallet.getPendingBalance());
                existingWallet.setUpdatedAt(wallet.getUpdatedAt());

                walletRepository.save(existingWallet);
        }

        private WalletResponse mapToWalletResponse(Wallet wallet) {
                return WalletResponse.builder()
                                .id(wallet.getId())
                                .balance(wallet.getBalance())
                                .pendingBalance(wallet.getPendingBalance())
                                .createdAt(wallet.getCreatedAt())
                                .updatedAt(wallet.getUpdatedAt())
                                .build();
        }

        private WalletTransactionResponse mapToWalletTransactionResponse(WalletTransaction transaction) {
                return WalletTransactionResponse.builder()
                                .id(transaction.getId())
                                .amount(transaction.getAmount())
                                .type(transaction.getType())
                                .status(transaction.getStatus())
                                .description(transaction.getDescription())
                                .createdAt(transaction.getCreatedAt())
                                .bookingId(transaction.getBooking() != null ? transaction.getBooking().getId() : null)
                                .build();
        }
}
