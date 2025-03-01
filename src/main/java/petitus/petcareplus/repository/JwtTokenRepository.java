package petitus.petcareplus.repository;

import org.springframework.data.repository.CrudRepository;
import petitus.petcareplus.model.JwtToken;

import java.util.Optional;
import java.util.UUID;

public interface JwtTokenRepository extends CrudRepository<JwtToken, UUID> {
    Optional<JwtToken> findByTokenOrRefreshToken(String token, String refreshToken);

    Optional<JwtToken> findByUserIdAndRefreshToken(UUID id, String refreshToken);

    Optional<JwtToken> findByUserIdAndToken(UUID id, String token);
}
