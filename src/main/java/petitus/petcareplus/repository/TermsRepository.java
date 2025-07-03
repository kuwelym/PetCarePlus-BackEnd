package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.TermsAndConditions;
import petitus.petcareplus.utils.enums.TermsType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermsRepository extends JpaRepository<TermsAndConditions, UUID> {

    Optional<TermsAndConditions> findByTypeAndLanguageAndIsActiveTrue(TermsType type, String language);

    List<TermsAndConditions> findByLanguageAndIsActiveTrueOrderByCreatedAtDesc(String language);

    @Query("SELECT t FROM TermsAndConditions t WHERE t.isActive = true ORDER BY t.type ASC, t.language ASC")
    List<TermsAndConditions> findByIsActiveTrueOrderByTypeAscLanguageAsc();

    List<TermsAndConditions> findByTypeAndIsActiveTrueOrderByVersionDesc(TermsType type);
}