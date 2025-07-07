package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @Query(value = """
            SELECT m FROM ChatMessage m
            WHERE (m.senderId = :userId1 AND m.recipientId = :userId2)
               OR (m.senderId = :userId2 AND m.recipientId = :userId1)
            ORDER BY m.createdAt DESC
            """, countQuery = """
        SELECT COUNT(m) FROM ChatMessage m
        WHERE (m.senderId = :userId1 AND m.recipientId = :userId2)
           OR (m.senderId = :userId2 AND m.recipientId = :userId1)
        """)
    Page<ChatMessage> findConversationBetweenUsers(
        @Param("userId1") UUID userId1,
         @Param("userId2") UUID userId2,
          Pageable pageable
          );

    @Query(value = """
            SELECT m FROM ChatMessage m
            WHERE ((m.senderId = :userId1 AND m.recipientId = :userId2)
               OR (m.senderId = :userId2 AND m.recipientId = :userId1))
               AND m.createdAt < :lastMessageTime
            ORDER BY m.createdAt DESC
            """, countQuery = """
        SELECT COUNT(m) FROM ChatMessage m
        WHERE ((m.senderId = :userId1 AND m.recipientId = :userId2)
           OR (m.senderId = :userId2 AND m.recipientId = :userId1))
           AND m.createdAt < :lastMessageTime
        """)
    Page<ChatMessage> findConversationBetweenUsersOlderThan(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2,
        @Param("lastMessageTime") LocalDateTime lastMessageTime,
        Pageable pageable
    );

    @Query(value = """
        SELECT COUNT(*) FROM chat_messages
        WHERE recipient_id = :userId AND is_read = false
        """, nativeQuery = true)
    long countUnreadMessages(@Param("userId") UUID userId);
    
    @Query(value = """
        SELECT DISTINCT
            CASE\s
                WHEN m.sender_id = :currentUserId THEN m.recipient_id
                ELSE m.sender_id
            END as user_id,
            (
                SELECT MAX(m2.created_at)
                FROM chat_messages m2
                WHERE (m2.sender_id = m.sender_id AND m2.recipient_id = m.recipient_id)
                   OR (m2.sender_id = m.recipient_id AND m2.recipient_id = m.sender_id)
            ) as last_message_time
        FROM chat_messages m
        WHERE m.sender_id = :currentUserId OR m.recipient_id = :currentUserId
        ORDER BY last_message_time DESC
        LIMIT :limit
       \s""", nativeQuery = true)
    List<Object[]> findAllConversationUsersWithTimes(
        @Param("currentUserId") UUID currentUserId,
        @Param("limit") int limit
    );
    
    @Query(value = """
        SELECT DISTINCT
            CASE\s
                WHEN m.sender_id = :currentUserId THEN m.recipient_id
                ELSE m.sender_id
            END as user_id,
            (
                SELECT MAX(m2.created_at)
                FROM chat_messages m2
                WHERE (m2.sender_id = m.sender_id AND m2.recipient_id = m.recipient_id)
                   OR (m2.sender_id = m.recipient_id AND m2.recipient_id = m.sender_id)
            ) as last_message_time
        FROM chat_messages m
        WHERE (m.sender_id = :currentUserId OR m.recipient_id = :currentUserId)
        AND (
            SELECT MAX(m2.created_at)
            FROM chat_messages m2
            WHERE (m2.sender_id = m.sender_id AND m2.recipient_id = m.recipient_id)
               OR (m2.sender_id = m.recipient_id AND m2.recipient_id = m.sender_id)
        ) < :lastMessageTime
        ORDER BY last_message_time DESC
        LIMIT :limit
       \s""", nativeQuery = true)
    List<Object[]> findAllConversationUsersWithKeyset(
        @Param("currentUserId") UUID currentUserId,
        @Param("lastMessageTime") LocalDateTime lastMessageTime,
        @Param("limit") int limit
    );
    
    @Query(value = """
        SELECT m
        FROM ChatMessage m
        WHERE (m.senderId = :userId1 AND m.recipientId = :userId2)
           OR (m.senderId = :userId2 AND m.recipientId = :userId1)
        ORDER BY m.createdAt DESC
        LIMIT 1
        """)
    ChatMessage findLatestMessageBetweenUsers(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );

    @Modifying
    @Query(value = """
        UPDATE chat_messages
        SET is_read = true, read_at = CURRENT_TIMESTAMP
        WHERE sender_id = :senderId AND recipient_id = :recipientId
        AND is_read = false
        RETURNING id
        """, nativeQuery = true)
    List<UUID> updateChatMessagesAsRead(
        @Param("senderId") UUID senderId,
        @Param("recipientId") UUID recipientId
    );

    /**
     * Get list of user IDs who have conversations with the specified user
     * Optimized query for presence notifications - only returns user IDs, no timestamps
     */
    @Query(value = """
        SELECT DISTINCT
            CASE 
                WHEN m.sender_id = :currentUserId THEN CAST(m.recipient_id AS VARCHAR)
                ELSE CAST(m.sender_id AS VARCHAR)
            END as user_id
        FROM chat_messages m
        WHERE m.sender_id = :currentUserId OR m.recipient_id = :currentUserId
        """, nativeQuery = true)
    List<String> findConversationPartnerIds(@Param("currentUserId") UUID currentUserId);
} 
