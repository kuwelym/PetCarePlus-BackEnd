package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.Pet;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.user.id = :userId")
    List<Pet> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.user.id = :userId")
    Page<Pet> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Pet p WHERE p.deletedAt IS NULL AND p.name LIKE %:name%")
    List<Pet> findByNameContaining(@Param("name") String name);

    @Query("SELECT COUNT(p) FROM Pet p WHERE p.deletedAt IS NULL AND p.user.id = :userId")
    Long countByUserId(@Param("userId") UUID userId);
}