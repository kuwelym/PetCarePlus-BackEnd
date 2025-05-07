package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.wallet.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    Optional<Wallet> findByUser(User user);
}