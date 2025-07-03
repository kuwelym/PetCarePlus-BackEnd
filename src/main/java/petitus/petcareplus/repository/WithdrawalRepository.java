package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import petitus.petcareplus.model.wallet.Withdrawal;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {

    Page<Withdrawal> findByProviderIdOrderByCreatedAtDesc(UUID providerId, Pageable pageable);

    Page<Withdrawal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Withdrawal> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM Withdrawal w " +
            "WHERE w.provider.id = :providerId " +
            "AND w.status NOT IN ('REJECTED', 'FAILED') " +
            "AND DATE(w.createdAt) = DATE(CURRENT_DATE)")
    BigDecimal getTodayWithdrawalTotal(@Param("providerId") UUID providerId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM Withdrawal w " +
            "WHERE w.provider.id = :providerId " +
            "AND w.status NOT IN ('REJECTED', 'FAILED') " +
            "AND YEAR(w.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(w.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal getMonthWithdrawalTotal(@Param("providerId") UUID providerId);

    @Query("SELECT COUNT(w) FROM Withdrawal w " +
            "WHERE w.provider.id = :providerId " +
            "AND w.status = 'PENDING'")
    Long countPendingWithdrawals(@Param("providerId") UUID providerId);
}