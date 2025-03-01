package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.JwtToken;
import petitus.petcareplus.repository.JwtTokenRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtTokenRepository jwtTokenRepository;

    private final MessageSourceService messageSourceService;

    public JwtToken findByUserIdAndRefreshToken(UUID id, String refreshToken) {
        return jwtTokenRepository.findByUserIdAndRefreshToken(id, refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("token")})));
    }

    public JwtToken findByUserIdAndToken(UUID id, String token) {
        return jwtTokenRepository.findByUserIdAndToken(id, token)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("token")})));
    }

    public JwtToken findByTokenOrRefreshToken(String token) {
        return jwtTokenRepository.findByTokenOrRefreshToken(token, token)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("not_found_with_param",
                        new String[]{messageSourceService.get("token")})));
    }

    public void save(JwtToken jwtToken) {
        jwtTokenRepository.save(jwtToken);
    }

    public void delete(JwtToken jwtToken) {
        jwtTokenRepository.delete(jwtToken);
    }
}