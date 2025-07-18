package petitus.petcareplus.model.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.model.spec.criteria.WithdrawalCriteria;
import petitus.petcareplus.model.wallet.Withdrawal;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class WithdrawalSpecification implements Specification<Withdrawal> {

    private final WithdrawalCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<Withdrawal> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Filter by status
        if (criteria.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
        }

        // Filter by amount range
        if (criteria.getAmountFrom() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("amount"), criteria.getAmountFrom()));
        }

        if (criteria.getAmountTo() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("amount"), criteria.getAmountTo()));
        }

        // Filter by bank name
        if (criteria.getBankName() != null && !criteria.getBankName().isBlank()) {
            String searchPattern = "%" + criteria.getBankName().toLowerCase() + "%";
            Path<String> bankNamePath = root.get("bankName");
            predicates.add(builder.like(builder.lower(bankNamePath), searchPattern));
        }

        // Filter by deleted status (for admin)
        if (criteria.getIsDeleted() != null) {
            if (criteria.getIsDeleted()) {
                // Only get withdrawals that have been deleted
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            } else {
                // Only get withdrawals that have not been deleted
                predicates.add(builder.isNull(root.get("deletedAt")));
            }
        } else {
            // Default behavior: only get withdrawals that have not been deleted
            predicates.add(builder.isNull(root.get("deletedAt")));
        }

        // Nếu không có predicate nào thì return null
        if (predicates.isEmpty()) {
            return null;
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }

}
