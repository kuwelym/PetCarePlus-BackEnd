package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final StringRedisTemplate redisTemplate;

    private static final long RATE_LIMIT_SECONDS = 30; // 30 seconds

    public boolean canResendVerification(String email) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String key = "resend_email_verification:" + email;

        if (redisTemplate.hasKey(key)) {
            return false;
        }

        ops.set(key, "1", Duration.ofSeconds(RATE_LIMIT_SECONDS));
        return true;
    }
}
