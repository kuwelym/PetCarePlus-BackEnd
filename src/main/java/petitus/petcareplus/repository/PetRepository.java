package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.Pet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID>, JpaSpecificationExecutor<Pet> {

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.id = :petId")
    Optional<Pet> findById(UUID petId);

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.userId = :userId")
    List<Pet> findByUserId(UUID userId);

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.userId = :ownerId ORDER BY p.createdAt DESC")
    Page<Pet> findByOwnerId(UUID ownerId, Pageable pageable);

    @Query("SELECT p.species as species, COUNT(p) as count FROM Pet p GROUP BY p.species")
    List<Object[]> countBySpeciesRaw();

    @Query("SELECT p.breed as breed, COUNT(p) as count FROM Pet p GROUP BY p.breed")
    List<Object[]> countByBreedRaw();

    default Map<String, Long> countBySpecies() {
        return countBySpeciesRaw().stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]));
    }

    default Map<String, Long> countByBreed() {
        return countByBreedRaw().stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]));
    }
}
