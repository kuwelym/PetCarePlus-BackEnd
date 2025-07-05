package petitus.petcareplus.model.wallet;

import jakarta.persistence.*;
import lombok.*;
import petitus.petcareplus.model.AbstractBaseEntity;
import petitus.petcareplus.model.User;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdrawal extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider; // Service provider

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee; // Phí rút tiền

    @Column(nullable = false)
    private BigDecimal netAmount; // Số tiền thực nhận = amount - fee

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    // Thông tin ngân hàng
    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    // Admin processing
    private String adminNote;
    private LocalDateTime processedAt;
    private String processedBy; // Admin username

    // Transaction reference
    private String transactionRef;
    private String rejectionReason;
}