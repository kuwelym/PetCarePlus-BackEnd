package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.model.FcmToken;
import petitus.petcareplus.repository.FcmTokenRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserService userService;

    @Transactional
    public void saveToken(String token) {
        UUID userId = userService.getCurrentUserId();
        // Delete existing token if it exists
        fcmTokenRepository.deleteByToken(token);
        
        // Create and save new token
        FcmToken fcmToken = FcmToken.builder()
                .userId(userId)
                .token(token)
                .build();
        
        fcmTokenRepository.save(fcmToken);
    }

    public List<String> getUserTokens(UUID userId) {
        return fcmTokenRepository.findByUserId(userId).stream()
                .map(FcmToken::getToken)
                .toList();
    }
} 