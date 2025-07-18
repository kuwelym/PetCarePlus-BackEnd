package petitus.petcareplus.model.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.criteria.ProviderServiceCriteria;

@Slf4j
@RequiredArgsConstructor
public final class ProviderServiceSpecification implements Specification<ProviderService> {
    private final ProviderServiceCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<ProviderService> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Search by service name or provider name
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String searchPattern = "%" + criteria.getQuery().toLowerCase() + "%";
            Join<ProviderService, DefaultService> serviceJoin = root.join("service");
            Join<ProviderService, User> providerJoin = root.join("provider");

            predicates.add(builder.or(
                    builder.like(builder.lower(serviceJoin.get("name")), searchPattern),
                    builder.like(builder.lower(providerJoin.get("name")), searchPattern),
                    builder.like(builder.lower(providerJoin.get("lastName")), searchPattern)));
        }

        // Filter by provider ID
        if (criteria.getProviderId() != null) {
            predicates.add(builder.equal(root.get("provider").get("id"), criteria.getProviderId()));
        }

        // Filter by service ID
        if (criteria.getServiceId() != null) {
            predicates.add(builder.equal(root.get("service").get("id"), criteria.getServiceId()));
        }

        // Filter by price range
        if (criteria.getMinCustomPrice() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("customPrice"), criteria.getMinCustomPrice()));
        }

        if (criteria.getMaxCustomPrice() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("customPrice"), criteria.getMaxCustomPrice()));
        }

        // Filter by date range
        if (criteria.getCreatedAtStart() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtStart()));
        }

        if (criteria.getCreatedAtEnd() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtEnd()));
        }

        // Filter by deleted status
        if (criteria.getIsDeleted() != null) {
            if (criteria.getIsDeleted()) {
                // Chỉ lấy những provider service đã bị xóa
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            } else {
                // Chỉ lấy những provider service chưa bị xóa
                predicates.add(builder.isNull(root.get("deletedAt")));
            }
        } else {
            // Mặc định chỉ lấy những provider service chưa bị xóa
            predicates.add(builder.isNull(root.get("deletedAt")));
        }

        // Nếu không có predicate nào thì return null
        if (predicates.isEmpty()) {
            return null;
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}