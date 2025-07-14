package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String REDIS_ONLINE_USERS_KEY = "chat:online_users";
    private static final int ONLINE_USER_TTL_SECONDS = 300; // 5 minutes TTL

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Handle user presence - mark user as online or offline
     */
    public void handleUserPresence(UUID userId, boolean isOnline) {
        try {
            String userIdStr = userId.toString();
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userIdStr;

            if (isOnline) {
                redisTemplate.opsForValue().set(redisKey, "online", Duration.ofSeconds(ONLINE_USER_TTL_SECONDS));
            } else {
                redisTemplate.delete(redisKey);
            }

        } catch (Exception e) {
            log.error("Error handling user presence for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Check if a user is currently online in Redis
     */
    public boolean isUserOnline(String userId) {
        try {
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.error("Error checking user online status in Redis for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle heartbeat - refresh user's online status
     */
    public void handleHeartbeat(UUID userId) {
        try {
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userId.toString();

            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                // Refresh the TTL without triggering presence broadcast
                System.out.println("User " + userId + " is online, refreshing TTL.");
                redisTemplate.expire(redisKey, Duration.ofSeconds(ONLINE_USER_TTL_SECONDS));
            } else {
                System.out.println("User " + userId + " is not online, marking as online now.");
                handleUserPresence(userId, true);
            }
        } catch (Exception e) {
            System.out.println("Error handling heartbeat for user " + userId + ": " + e.getMessage());
            log.error("Error handling heartbeat for user {}: {}", userId, e.getMessage(), e);
            handleUserPresence(userId, true);
        }
    }
}
