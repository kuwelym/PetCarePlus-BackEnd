package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.ServiceReview;
import petitus.petcareplus.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class ServiceReviewSpecification {

    // Search by comment or user name
    public static Specification<ServiceReview> searchByQuery(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + query.toLowerCase() + "%";
            Join<ServiceReview, User> userJoin = root.join("user");

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("name")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("lastName")), searchPattern));
        };
    }

    // Filter by reviewer (user who wrote the review)
    public static Specification<ServiceReview> byUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    // Filter by provider
    public static Specification<ServiceReview> byProviderId(UUID providerId) {
        return (root, query, criteriaBuilder) -> {
            if (providerId == null) {
                return criteriaBuilder.conjunction();
            }
            Join<ServiceReview, ProviderService> providerServiceJoin = root.join("providerService");
            return criteriaBuilder.equal(providerServiceJoin.get("provider").get("id"), providerId);
        };
    }

    // Filter by service
    public static Specification<ServiceReview> byServiceId(UUID serviceId) {
        return (root, query, criteriaBuilder) -> {
            if (serviceId == null) {
                return criteriaBuilder.conjunction();
            }
            Join<ServiceReview, ProviderService> providerServiceJoin = root.join("providerService");
            Join<ProviderService, DefaultService> serviceJoin = providerServiceJoin.join("service");
            return criteriaBuilder.equal(serviceJoin.get("id"), serviceId);
        };
    }

    // Filter by provider service
    public static Specification<ServiceReview> byProviderServiceId(UUID providerServiceId) {
        return (root, query, criteriaBuilder) -> {
            if (providerServiceId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("providerService").get("id"), providerServiceId);
        };
    }

    // Filter by rating range
    public static Specification<ServiceReview> ratingGreaterThanOrEqual(Integer minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
        };
    }

    public static Specification<ServiceReview> ratingLessThanOrEqual(Integer maxRating) {
        return (root, query, criteriaBuilder) -> {
            if (maxRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating);
        };
    }

    // Filter by date range
    public static Specification<ServiceReview> createdAfter(LocalDateTime createdAtStart) {
        return (root, query, criteriaBuilder) -> {
            if (createdAtStart == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart);
        };
    }

    public static Specification<ServiceReview> createdBefore(LocalDateTime createdAtEnd) {
        return (root, query, criteriaBuilder) -> {
            if (createdAtEnd == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd);
        };
    }

    // Filter by comment existence
    public static Specification<ServiceReview> hasComment(Boolean hasComment) {
        return (root, query, criteriaBuilder) -> {
            if (hasComment == null) {
                return criteriaBuilder.conjunction();
            }

            if (hasComment) {
                return criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("comment")),
                        criteriaBuilder.notEqual(root.get("comment"), ""));
            } else {
                return criteriaBuilder.or(

                        criteriaBuilder.isNull(root.get("comment")),
                        criteriaBuilder.equal(root.get("comment"), ""));
            }
        };
    }

    // Filter by deleted status
    public static Specification<ServiceReview> isDeleted(Boolean isDeleted) {
        return (root, query, criteriaBuilder) -> {
            if (isDeleted == null) {
                return criteriaBuilder.conjunction();
            }

            if (isDeleted) {
                return criteriaBuilder.isNotNull(root.get("deletedAt"));
            } else {
                return criteriaBuilder.isNull(root.get("deletedAt"));
            }
        };
    }

    // Active reviews only (not deleted)
    public static Specification<ServiceReview> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    // Filter by specific rating
    public static Specification<ServiceReview> byRating(Integer rating) {
        return (root, query, criteriaBuilder) -> {
            if (rating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("rating"), rating);
        };
    }
}