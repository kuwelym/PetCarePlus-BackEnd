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
import petitus.petcareplus.model.ServiceReview;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.criteria.ServiceReviewCriteria;

@Slf4j
@RequiredArgsConstructor
public final class ServiceReviewSpecification implements Specification<ServiceReview> {
    private final ServiceReviewCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<ServiceReview> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Search by comment or user name
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String searchPattern = "%" + criteria.getQuery().toLowerCase() + "%";
            Join<ServiceReview, User> userJoin = root.join("user");

            predicates.add(builder.or(
                    builder.like(builder.lower(root.get("comment")), searchPattern),
                    builder.like(builder.lower(userJoin.get("name")), searchPattern),
                    builder.like(builder.lower(userJoin.get("lastName")), searchPattern)));
        }

        // Filter by reviewer (user who wrote the review)
        if (criteria.getUserId() != null) {
            predicates.add(builder.equal(root.get("user").get("id"), criteria.getUserId()));
        }

        // Filter by provider
        if (criteria.getProviderId() != null) {
            Join<ServiceReview, ProviderService> providerServiceJoin = root.join("providerService");
            predicates.add(builder.equal(providerServiceJoin.get("provider").get("id"), criteria.getProviderId()));
        }

        // Filter by service
        if (criteria.getServiceId() != null) {
            Join<ServiceReview, ProviderService> providerServiceJoin = root.join("providerService");
            Join<ProviderService, DefaultService> serviceJoin = providerServiceJoin.join("service");
            predicates.add(builder.equal(serviceJoin.get("id"), criteria.getServiceId()));
        }

        // Filter by provider service
        if (criteria.getProviderServiceId() != null) {
            predicates.add(builder.equal(root.get("providerService").get("id"), criteria.getProviderServiceId()));
        }

        // Filter by specific rating
        if (criteria.getRating() != null) {
            predicates.add(builder.equal(root.get("rating"), criteria.getRating()));
        }

        // Filter by rating range
        if (criteria.getMinRating() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("rating"), criteria.getMinRating()));
        }

        if (criteria.getMaxRating() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("rating"), criteria.getMaxRating()));
        }

        // Filter by date range
        if (criteria.getCreatedAtStart() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtStart()));
        }

        if (criteria.getCreatedAtEnd() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtEnd()));
        }

        // Filter by comment existence
        if (criteria.getHasComment() != null) {
            if (criteria.getHasComment()) {
                predicates.add(builder.and(
                        builder.isNotNull(root.get("comment")),
                        builder.notEqual(root.get("comment"), "")));
            } else {
                predicates.add(builder.or(
                        builder.isNull(root.get("comment")),
                        builder.equal(root.get("comment"), "")));
            }
        }

        // Filter by deleted status
        if (criteria.getIsDeleted() != null) {
            if (criteria.getIsDeleted()) {
                // Chỉ lấy những review đã bị xóa
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            } else {
                // Chỉ lấy những review chưa bị xóa
                predicates.add(builder.isNull(root.get("deletedAt")));
            }
        } else {
            // Mặc định chỉ lấy những review chưa bị xóa
            predicates.add(builder.isNull(root.get("deletedAt")));
        }

        // Nếu không có predicate nào thì return null
        if (predicates.isEmpty()) {
            return null;
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}