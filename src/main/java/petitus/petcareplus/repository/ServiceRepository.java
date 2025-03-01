package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.PetService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<PetService, UUID> {
    Optional<PetService> findByName(String name);
    List<PetService> findByNameContainingIgnoreCase(String name);
}