package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
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



        if (criteria.getQuery() != null) {
            String q = String.format("%%%s%%", criteria.getQuery().toLowerCase());
            Join<Profile, User> userJoin = root.join("user", JoinType.LEFT);
            predicates.add(
                    builder.or(
                            builder.like(builder.lower(userJoin.get("name")), q),
                            builder.like(builder.lower(userJoin.get("lastName")), q)
                    )
            );
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

        if (predicates.isEmpty()) {
            return builder.conjunction();
        }

        query.where(predicates.toArray(new Predicate[0]));

        return query.distinct(true).getRestriction();
    }
}
