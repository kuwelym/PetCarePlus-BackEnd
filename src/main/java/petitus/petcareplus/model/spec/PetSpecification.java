package petitus.petcareplus.model.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import petitus.petcareplus.model.Pet;
import petitus.petcareplus.model.spec.criteria.PetCriteria;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class PetSpecification implements Specification<Pet> {
    private final PetCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<Pet> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder criteriaBuilder) {
        if (criteria == null) {
            return null;
        }
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String searchTerm = "%" + criteria.getQuery().toLowerCase() + "%";
            Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchTerm);
            Predicate speciesPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("species")), searchTerm);
            Predicate breedPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("breed")), searchTerm);

            predicates.add(criteriaBuilder.or(namePredicate, speciesPredicate, breedPredicate));
        }

        if (criteria.getSpecies() != null && !criteria.getSpecies().trim().isEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get("species"), criteria.getSpecies()));
        }

        if (criteria.getBreed() != null && !criteria.getBreed().trim().isEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get("breed"), criteria.getBreed()));
        }

        if (criteria.getGender() != null && !criteria.getGender().trim().isEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get("gender"), criteria.getGender()));
        }

        if (criteria.getMinAge() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("age"), criteria.getMinAge()));
        }

        if (criteria.getMaxAge() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("age"), criteria.getMaxAge()));
        }

        if (criteria.getOwnerId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("ownerId"), criteria.getOwnerId()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

}