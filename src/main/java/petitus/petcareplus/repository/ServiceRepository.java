package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.DefaultService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<DefaultService, UUID> {
    Optional<DefaultService> findByName(String name);

    List<DefaultService> findByNameContainingIgnoreCase(String name);
}