package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import petitus.petcareplus.model.DefaultService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository
        extends JpaRepository<DefaultService, UUID>, JpaSpecificationExecutor<DefaultService> {

    @Query("SELECT s FROM DefaultService s WHERE s.deletedAt IS NULL AND s.name = :name")
    Optional<DefaultService> findByName(@Param("name") String name);

    List<DefaultService> findByNameContainingIgnoreCase(String name);

    // find all services that are not deleted
    default List<DefaultService> findAllActiveServices() {
        return findAll().stream()
                .filter(service -> service.getDeletedAt() == null)
                .toList();
    }

    @Query("SELECT s FROM DefaultService s WHERE s.deletedAt IS NULL AND s.id = :id")
    Optional<DefaultService> findById(@Param("id") UUID id);

}