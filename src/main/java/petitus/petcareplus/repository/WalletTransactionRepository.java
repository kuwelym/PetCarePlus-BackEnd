package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.model.wallet.WalletTransaction;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWallet(Wallet wallet, Pageable pageable);
}