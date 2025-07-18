package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import petitus.petcareplus.model.ProviderService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderServiceRepository
        extends JpaRepository<ProviderService, UUID>, JpaSpecificationExecutor<ProviderService> {

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.provider.id = :providerId")
    List<ProviderService> findByProviderId(UUID providerId);

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.service.id = :serviceId")
    List<ProviderService> findByServiceId(UUID serviceId);

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.provider.id = :providerId")
    List<ProviderService> findActiveServicesByProviderId(UUID providerId);

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.provider.id = :providerId AND ps.service.id = :serviceId")
    Optional<ProviderService> findActiveByProviderIdAndServiceId(UUID providerId, UUID serviceId);

    // Tìm bao gồm cả soft deleted
    Optional<ProviderService> findByProviderIdAndServiceId(UUID providerId, UUID serviceId);

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL")
    List<ProviderService> findAllActiveService();

    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.id = :id")
    Optional<ProviderService> findById(UUID id);
}