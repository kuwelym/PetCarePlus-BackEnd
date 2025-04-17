package petitus.petcareplus.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.wallet.WalletResponse;
import petitus.petcareplus.exceptions.DataExistedException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.repository.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    public WalletResponse getWalletByUser(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user ID: " + userId));
        return mapToWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse createWallet(UUID userId) {

        // Check if wallet already exists for the user
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new DataExistedException("Wallet already exists for user ID: " + userId);
        }

        Wallet wallet = Wallet.builder()
                .user(userService.getUser())
                .balance(BigDecimal.ZERO) // Initialize balance to 0
                .pendingBalance(BigDecimal.ZERO) // Initialize pending balance to 0
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        return mapToWalletResponse(savedWallet);
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
}
