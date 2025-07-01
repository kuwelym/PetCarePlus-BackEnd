package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.model.spec.criteria.ProfileCriteria;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class ProfileFilterSpecification implements Specification<Profile> {
    private final ProfileCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<Profile> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getIsServiceProvider() != null && criteria.getIsServiceProvider()) {
            Path<ServiceProviderProfile> serviceProviderProfile = root.get("serviceProviderProfile");
            if (criteria.getQuery() != null) {
                String q = String.format("%%%s%%", criteria.getQuery());
                predicates.add(
                        builder.or(
                                builder.like(builder.lower(root.get("about")), q)
                        )
                );
            }

            if (criteria.getRating() != null && criteria.getRating() >= 0){
                predicates.add(
                        builder.between(
                                serviceProviderProfile.get("rating"),
                                criteria.getRating(),
                                criteria.getRating() + 1
                        )
                );
            }

            if (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) {
                predicates.add(
                        serviceProviderProfile.get("skills").in(criteria.getSkills())
                );
            }

            // Example of handling JSONB field (availableTime) using PostgreSQL functions
            if (criteria.getAvailableAtStart() != null) {
                predicates.add(
                        builder.greaterThanOrEqualTo(serviceProviderProfile.get("availableTime"), criteria.getAvailableAtStart())
                );
            }

            if (criteria.getAvailableAtEnd() != null) {
                predicates.add(
                        builder.lessThanOrEqualTo(serviceProviderProfile.get("availableTime"), criteria.getAvailableAtEnd())
                );
            }

            if (criteria.getAvailableTime() != null) {
                predicates.add(
                        (Predicate) builder.function("jsonb_exists", Boolean.class, serviceProviderProfile.get("availableTime"), builder.literal(criteria.getAvailableTime()))
                );
            }
        }

        if (criteria.getLocation() != null) {
            predicates.add(
                    builder.like(builder.lower(root.get("location")), "%" + criteria.getLocation().toLowerCase() + "%")
            );
        }

        if (criteria.getCreatedAtStart() != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtStart())
            );
        }

        if (criteria.getCreatedAtEnd() != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtEnd())
            );
        }

        if (criteria.getIsServiceProvider() != null) {
            predicates.add(
                    builder.equal(root.get("isServiceProvider"), criteria.getIsServiceProvider())
            );
        }

        if (predicates.isEmpty()) {
            return builder.conjunction();
        }

        query.where(predicates.toArray(new Predicate[0]));

        return query.distinct(true).getRestriction();
    }
}
