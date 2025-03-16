package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;

public interface ServiceProviderProfileRepository extends JpaRepository<ServiceProviderProfile, Long>, JpaSpecificationExecutor<Profile> {
}
