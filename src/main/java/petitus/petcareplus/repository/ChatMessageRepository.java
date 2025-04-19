package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.ChatMessage;

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
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.recipientId = :userId AND m.isRead = false
        """, nativeQuery = true)
    long countUnreadMessages(@Param("userId") UUID userId);
} 