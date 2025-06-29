package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.ChatImageMessage;

import java.util.UUID;

@Repository
public interface ChatImageMessageRepository extends JpaRepository<ChatImageMessage, UUID> {
} 
