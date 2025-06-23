package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProviderServiceSpecification {

    // Search by service name or provider name
    public static Specification<ProviderService> searchByQuery(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + query.toLowerCase() + "%";
            Join<ProviderService, DefaultService> serviceJoin = root.join("service");
            Join<ProviderService, User> providerJoin = root.join("provider");

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("name")), searchPattern),
                    // criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("description")),
                    // searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(providerJoin.get("name")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(providerJoin.get("lastName")), searchPattern));
        };
    }

    // Filter by provider ID
    public static Specification<ProviderService> byProviderId(UUID providerId) {
        return (root, query, criteriaBuilder) -> {
            if (providerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("provider").get("id"), providerId);
        };
    }

    // Filter by service ID
    public static Specification<ProviderService> byServiceId(UUID serviceId) {
        return (root, query, criteriaBuilder) -> {
            if (serviceId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("service").get("id"), serviceId);
        };
    }

    // Filter by price range
    public static Specification<ProviderService> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("customPrice"), minPrice);
        };
    }

    public static Specification<ProviderService> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("customPrice"), maxPrice);
        };
    }

    // Filter by provider location
    public static Specification<ProviderService> byLocation(String location) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (location == null || location.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            Join<ProviderService, User> providerJoin = root.join("provider");
            Join<User, Profile> profileJoin = providerJoin.join("profile");

            String searchPattern = "%" + location.toLowerCase() + "%";
            return criteriaBuilder.like(
                    criteriaBuilder.lower(profileJoin.get("location")),
                    searchPattern);
        };
    }

    // Filter by date range
    public static Specification<ProviderService> createdAfter(LocalDateTime createdAtStart) {
        return (root, query, criteriaBuilder) -> {
            if (createdAtStart == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAtStart);
        };
    }

    public static Specification<ProviderService> createdBefore(LocalDateTime createdAtEnd) {
        return (root, query, criteriaBuilder) -> {
            if (createdAtEnd == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdAtEnd);
        };
    }

    // Filter by deleted status
    public static Specification<ProviderService> isDeleted(Boolean isDeleted) {
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

    // Active services only (not deleted)
    public static Specification<ProviderService> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }
}