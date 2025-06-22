package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.spec.criteria.ServiceCriteria;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class ServiceFilterSpecification implements Specification<DefaultService> {
    private final ServiceCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<DefaultService> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Search by name or description
        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String searchPattern = "%" + criteria.getQuery().toLowerCase() + "%";
            predicates.add(
                    builder.or(
                            builder.like(builder.lower(root.get("name")), searchPattern),
                            builder.like(builder.lower(root.get("description")), searchPattern)));
        }

        // Filter by minimum price
        if (criteria.getMinPrice() != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(root.get("basePrice"), criteria.getMinPrice()));
        }

        // Filter by maximum price
        if (criteria.getMaxPrice() != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(root.get("basePrice"), criteria.getMaxPrice()));
        }

        if (predicates.isEmpty()) {
            return builder.conjunction();
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }

    public static Specification<DefaultService> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String searchPattern = "%" + name.toLowerCase() + "%";
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    searchPattern);
        };
    }
}