package petitus.petcareplus.model.wallet;

import jakarta.persistence.*;
import lombok.*;
import petitus.petcareplus.model.AbstractBaseEntity;
import petitus.petcareplus.model.User;

@Entity
@Table(name = "provider_bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderBankAccount extends AbstractBaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Builder.Default
    private Boolean isDefault = true;

    @Builder.Default
    private Boolean isVerified = false;
}