package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveChatService {

    private static final String REDIS_ACTIVE_CHATS_KEY = "chat:active_chats";
    private static final int ACTIVE_CHAT_TTL_SECONDS = 600; // 10 minutes TTL for active chats

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Handle active chat tracking - when user enters or leaves a specific chat conversation
     */
    public void handleActiveChat(UUID userId, UUID otherUserId, boolean isActive) {
        try {
            String redisKey = REDIS_ACTIVE_CHATS_KEY + ":" + userId.toString() + ":" + otherUserId.toString();
            
            if (isActive) {
                redisTemplate.opsForValue().set(redisKey, "active", Duration.ofSeconds(ACTIVE_CHAT_TTL_SECONDS));
            } else {
                redisTemplate.delete(redisKey);
            }
        } catch (Exception e) {
            log.error("Error handling active chat for users {} and {}: {}", userId, otherUserId, e.getMessage(), e);
        }
    }

    /**
     * Check if a user is currently in an active chat with another user
     */
    public boolean isUserInActiveChatWith(UUID userId, UUID otherUserId) {
        try {
            String redisKey = REDIS_ACTIVE_CHATS_KEY + ":" + userId.toString() + ":" + otherUserId.toString();
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.error("Error checking active chat status for users {} and {}: {}", userId, otherUserId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clean up all active chats for a user (called on disconnect)
     */
    public void cleanupUserActiveChats(UUID userId) {
        try {
            String pattern = REDIS_ACTIVE_CHATS_KEY + ":" + userId.toString() + ":*";
            Set<String> activeChatsKeys = redisTemplate.keys(pattern);
            
            if (!activeChatsKeys.isEmpty()) {
                redisTemplate.delete(activeChatsKeys);
            }
        } catch (Exception e) {
            log.error("Error cleaning up active chats for user {}: {}", userId, e.getMessage(), e);
        }
    }
} 
