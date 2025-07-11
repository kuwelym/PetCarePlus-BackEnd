package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.model.spec.criteria.ServiceProviderProfileCriteria;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class ServiceProviderProfileFilterSpecification implements Specification<ServiceProviderProfile> {
    private final ServiceProviderProfileCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<ServiceProviderProfile> root,
                                 @NonNull final CriteriaQuery<?> query,
                                 @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getRating() != null && criteria.getRating() >= 0){
            predicates.add(
                    builder.between(
                            root.get("rating"),
                            criteria.getRating(),
                            criteria.getRating() + 1
                    )
            );
        }

        if (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) {
            predicates.add(
                    root.get("skills").in(criteria.getSkills())
            );
        }

        // Example of handling JSONB field (availableTime) using PostgreSQL functions
        if (criteria.getAvailableAtStart() != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(root.get("availableTime"), criteria.getAvailableAtStart())
            );
        }

        if (criteria.getAvailableAtEnd() != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(root.get("availableTime"), criteria.getAvailableAtEnd())
            );
        }

        if (criteria.getAvailableTime() != null) {
            predicates.add(
                    (Predicate) builder.function("jsonb_exists", Boolean.class, root.get("availableTime"), builder.literal(criteria.getAvailableTime()))
            );
        }

        if (criteria.getQuery() != null) {
            String q = String.format("%%%s%%", criteria.getQuery().toLowerCase());
            Join<ServiceProviderProfile, Profile> profileJoin = root.join("profile", JoinType.LEFT);
            Join<Profile, User> userJoin = profileJoin.join("user", JoinType.LEFT);
            predicates.add(
                    builder.or(
                            builder.like(builder.lower(userJoin.get("name")), q),
                            builder.like(builder.lower(userJoin.get("lastName")), q),
                            builder.like(builder.lower(root.get("businessName")), q),
                            builder.like(builder.lower(root.get("businessBio")), q)
                    )
            );
        }

        if (criteria.getLocation() != null) {
            Join<ServiceProviderProfile, Profile> profileJoin = root.join("profile", JoinType.LEFT);
            predicates.add(
                    builder.like(builder.lower(profileJoin.get("location")), "%" + criteria.getLocation().toLowerCase() + "%")
            );
        }

        if (criteria.getBusinessAddress() != null) {
            predicates.add(
                    builder.like(builder.lower(root.get("businessAddress")), "%" + criteria.getBusinessAddress().toLowerCase() + "%")
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

        if (predicates.isEmpty()) {
            return builder.conjunction();
        }

        query.where(predicates.toArray(new Predicate[0]));

        return query.distinct(true).getRestriction();
    }
} 
