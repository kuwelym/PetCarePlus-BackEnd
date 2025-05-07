package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.FcmToken;

import java.util.List;
import java.util.UUID;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    List<FcmToken> findByUserId(UUID userId);
    void deleteByToken(String token);
} 