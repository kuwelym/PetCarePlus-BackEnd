package petitus.petcareplus.model.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.model.Booking;
import petitus.petcareplus.model.spec.criteria.BookingCriteria;

@RequiredArgsConstructor
public final class BookingFilterSpecification implements Specification<Booking> {
    private final BookingCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<Booking> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Search by username or provider name
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String searchPattern = "%" + criteria.getQuery().toLowerCase() + "%";
            Path<String> usernamePath = root.get("user").get("name");
            Path<String> userLastNamePath = root.get("user").get("lastName");
            Path<String> providerNamePath = root.get("provider").get("name");
            Path<String> providerLastNamePath = root.get("provider").get("lastName");

            predicates.add(builder.or(
                    builder.like(builder.lower(usernamePath), searchPattern),
                    builder.like(builder.lower(userLastNamePath), searchPattern),
                    builder.like(builder.lower(providerNamePath), searchPattern),
                    builder.like(builder.lower(providerLastNamePath), searchPattern)));
        }

        // Filter by email
        if (criteria.getMail() != null && !criteria.getMail().trim().isEmpty()) {
            String emailPattern = "%" + criteria.getMail().toLowerCase() + "%";
            Path<String> emailPath = root.get("user").get("email");
            predicates.add(builder.like(builder.lower(emailPath), emailPattern));
        }

        // Filter by status
        if (criteria.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
        }

        // Filter by payment status
        if (criteria.getPaymentStatus() != null) {
            predicates.add(builder.equal(root.get("paymentStatus"), criteria.getPaymentStatus()));
        }

        // Filter by user ID
        if (criteria.getUserId() != null && !criteria.getUserId().toString().trim().isEmpty()) {
            predicates.add(builder.equal(root.get("user").get("id"), criteria.getUserId()));
        }

        // Filter theo provider ID (service provider)
        if (criteria.getProviderId() != null && !criteria.getProviderId().toString().trim().isEmpty()) {
            predicates.add(builder.equal(root.get("provider").get("id"), criteria.getProviderId()));
        }

        // Filter theo deleted status (cho admin)
        if (criteria.getIsDeleted() != null) {
            if (criteria.getIsDeleted()) {
                // Chỉ lấy những booking đã bị xóa
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            } else {
                // Chỉ lấy những booking chưa bị xóa
                predicates.add(builder.isNull(root.get("deletedAt")));
            }
        } else {
            // Mặc định chỉ lấy những booking chưa bị xóa
            predicates.add(builder.isNull(root.get("deletedAt")));
        }

        // Nếu không có predicate nào thì return null
        if (predicates.isEmpty()) {
            return null;
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }

}
