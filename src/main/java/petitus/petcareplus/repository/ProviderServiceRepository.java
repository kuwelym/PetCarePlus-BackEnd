package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.ProviderServiceId;

import java.util.List;
import java.util.UUID;

public interface ProviderServiceRepository extends JpaRepository<ProviderService, ProviderServiceId> {
    List<ProviderService> findByProviderId(UUID providerId);
    
    List<ProviderService> findByServiceId(UUID serviceId);
    
    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.provider.id = :providerId")
    List<ProviderService> findActiveServicesByProviderId(UUID providerId);
    
    @Query("SELECT ps FROM ProviderService ps WHERE ps.deletedAt IS NULL AND ps.service.id = :serviceId")
    List<ProviderService> findProvidersByServiceId(UUID serviceId);
}